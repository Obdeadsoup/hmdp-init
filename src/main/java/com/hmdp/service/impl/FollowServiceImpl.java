package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.lang.invoke.LambdaConversionException;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    /**
     * Feed流部分还需要补充逻辑:用户关注博主后,需向用户Feed收件箱中加入该博主在关注前发布博客
     * 原本是需要注入BlogService,但BlogService中本身就需要FollowService注入
     * 为避免循环注入依赖,这里用BlogMapper
     * 并且不全部推送,只补入最近更新的20条blog
     */
    @Resource
    private BlogMapper blogMapper;
    private static final long INITIAL_FEED_SIZE = 20L;

    /**
     * IFollowService方法实现区
     */
    @Override
    public Result follow(Long followUserId,Boolean isFollow){
        UserDTO loginUser = UserHolder.getUser();

        if(loginUser==null){
            return Result.fail("请先登录");
        }
        if(followUserId==null){
            return Result.fail("关注用户id不能为空");
        }
        if(isFollow==null){
            return Result.fail("关注状态不能为空");
        }

        Long userId=loginUser.getId();
        if(userId.equals(followUserId)){
            return Result.fail("不能关注自己");
        }
        User targetUser=userService.getById(followUserId);
        if(targetUser==null){
            return Result.fail("关注的用户不存在");
        }

        // 拼接redis key,根据isFollow决定是新增member还是删除member
        String key=FOLLOWS_KEY+userId;
        if(isFollow){
            return doFollow(userId,followUserId,key);
        }
        return doUnFollow(userId,followUserId,key);
    }
    /**
     * 关注辅助函数
     */
    private Result doFollow(
            Long userId,
            Long followUserId,
            String key
    ){
        // 1. 先检查是否已经关注
        long count=query()
                .eq("user_id",userId)
                .eq("follow_user_id",followUserId)
                .count();
        if(count>0){
            // count大于0说明已有关系,在Redis中补记录
            stringRedisTemplate.opsForSet().add(
                    key,
                    followUserId.toString()
            );
            // 幂等补偿,也做一遍Feed收件箱重填,ZSet确保不会有重复
            seedHistoricalBlogs(userId, followUserId);

            return Result.ok();
        }
        // 2. 数据库中不存在则新建关注关系
        Follow follow=new Follow()
                .setUserId(userId)
                .setFollowUserId(followUserId);
        
        // 3. 写入数据库
        boolean success=save(follow);
        if(!success){
            return Result.fail("关注失败");
        }

        // 4. 写入数据库成功后写入Redis Set
        stringRedisTemplate.opsForSet().add(
                key,
                followUserId.toString()
        );
        // 5. 将该博主的近20条博客补全到当前用户Feed收件箱
        seedHistoricalBlogs(userId,followUserId);

        return Result.ok();
    }
    /**
     * 取关辅助方法
     */
    private Result doUnFollow(
            Long userId,
            Long followUserId,
            String key
    ){  
        // 1. 查询数据库并删除该记录
        remove(query()
                .eq("user_id",userId)
                .eq("follow_user_id",followUserId)
                .getWrapper());
        // 2. 无论数据库中是否有该关系记录都直接删除Redis缓存保证幂等性
        stringRedisTemplate.opsForSet().remove(
                key,
                followUserId.toString()
        );
        removeBlogsAfterUnfollow(userId,followUserId);

        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId){
        // 这个业务的逻辑其实就是只要在tb_follow表查是否存在对应关系记录
        UserDTO loginUser=UserHolder.getUser();

        if(loginUser==null){
            return Result.fail("请先登录");
        }
        if(followUserId==null||followUserId<1){
            return Result.fail("目标用户ID不合法");
        }
        long count=query()
                .eq("user_id",loginUser.getId())
                .eq("follow_user_id",followUserId)
                .count();
        return Result.ok(count>0);
    }

    @Override
    public Result followCommons(Long otherUserId){
        UserDTO loginUser=UserHolder.getUser();

        if(loginUser==null){
            return Result.fail("请先登录");
        }
        if(otherUserId==null||otherUserId<1){
            return Result.fail("目标用户ID不合法");
        }
        if(loginUser.getId().equals(otherUserId)){
            return Result.ok(Collections.emptyList());
        }
        // 利用Redis Set做交集运算
        String currentKey=FOLLOWS_KEY+loginUser.getId();
        String otherKey=FOLLOWS_KEY+otherUserId;
        Set<String> commonIds=stringRedisTemplate.opsForSet() 
                .intersect(currentKey,otherKey);

        if(commonIds==null||commonIds.isEmpty()){
            return Result.ok(Collections.emptyList());
        }
        List<Long> userIds=commonIds.stream()  
                .map(Long::valueOf)
                .collect(Collectors.toList());
        List<User> users=userService.listByIds(userIds);

        List<UserDTO> result=users.stream()
                .map(user->BeanUtil.copyProperties(
                        user,
                        UserDTO.class
                ))
                .collect(Collectors.toList());

        return Result.ok(result);
    }

    /**
     * 内部私有方法, 关注成功后调用 ,将被关注博主的博客推送到用户的Feed收件箱
     */
    private void seedHistoricalBlogs(long userId,long followUserId){
        Page<Blog> page=new Page<>(1,INITIAL_FEED_SIZE);

        LambdaQueryWrapper<Blog> wrapper=new LambdaQueryWrapper<Blog>()
                .eq(Blog::getUserId,followUserId)
                .orderByDesc(Blog::getCreateTime)
                .orderByDesc(Blog::getId);
        blogMapper.selectPage(page,wrapper);
        
        if(page.getRecords().isEmpty()){
            return;
        }

        String feedKey=FEED_KEY+ userId;

        for(Blog blog:page.getRecords()){
            long publishTime=blog.getCreateTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            stringRedisTemplate.opsForZSet().add(
                    feedKey,
                    blog.getId().toString(),
                    publishTime
            );
        }
    }
    /**
     * 内部私有方法,取关成功后调用,在当前用户Feed中清除被取关用户所有博客
     */
    private void removeBlogsAfterUnfollow(long userId,long followUserId){
        LambdaQueryWrapper<Blog> wrapper=new LambdaQueryWrapper<>();
        wrapper.select(Blog::getId)
                .eq(Blog::getUserId,followUserId);
        List<Blog> blogs=blogMapper.selectList(wrapper);

        if(blogs==null|| blogs.isEmpty()){
            return;
        }

        Object[] blogIds=blogs.stream()
                .map(Blog::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toArray();
        if(blogIds.length==0){
            return;
        }
        String feedKey=FEED_KEY+userId;
        stringRedisTemplate.opsForZSet()
                .remove(feedKey,blogIds);
    }
}
