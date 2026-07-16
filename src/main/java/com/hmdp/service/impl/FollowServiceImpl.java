package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.FOLLOWS_KEY;

@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;

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

}
