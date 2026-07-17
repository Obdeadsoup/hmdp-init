package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import com.hmdp.entity.Follow;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import static com.hmdp.utils.RedisConstants.*;
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Feed流
    @Resource
    private IFollowService followService;

    /**
     * 其实这几个方法通用逻辑都是先用Page进行query查询数据库得到博客信息,然后填充博客信息里缺失的User信息
     * 这里就以第一个查询热门博客为例进行注释说明, 后续实现均简单说明
     */
    @Override
    public Result queryHotBlog(Integer current) {
        
        int pageNumber = normalizeCurrent(current);
        // MyBatis-Plus提供的Page容器 ,再利用List获取records
        Page<Blog> page=query()
                .orderByDesc("liked")
                .orderByDesc("id")
                .page(new Page<>(
                        pageNumber,
                        SystemConstants.MAX_PAGE_SIZE
                ));
        List<Blog> blogs=page.getRecords();

        // 调用内部批量填充User信息的方法
        fillBlogUsers(blogs);
        
        return Result.ok(blogs);
    }

    @Override
    public Result queryBlogById(Long id) {
        if(id==null||id<1){
            return Result.fail("博客ID不存在");
        }
        Blog blog=getById(id);
        if(blog==null){
            return Result.fail("博客不存在");
        }
        fillBlogUser(blog);

        return Result.ok(blog);
    }

    @Override
    public Result queryMyBlog(Integer current) {
        UserDTO loginUser=UserHolder.getUser();

        if(loginUser==null){
            return Result.fail("当前未登录");
        }
        int pageNumber = normalizeCurrent(current);
        Page<Blog> page=query()
                .eq("user_id",loginUser.getId())
                .orderByDesc("create_time")
                .orderByDesc("id")
                .page(new Page<>(
                        pageNumber,
                        SystemConstants.MAX_PAGE_SIZE
                ));
        List<Blog> blogs=page.getRecords();
        fillBlogUsers(blogs);

        return Result.ok(blogs);
    }

    @Override
    public Result queryBlogByUserId(Long userId, Integer current) {
        if(userId==null||userId<1){
            return Result.fail("用户ID不存在");
        }
        int pageNumber=normalizeCurrent(current);
        Page<Blog> page=query()
                .eq("user_id",userId)
                .orderByDesc("create_time")
                .orderByDesc("id")
                .page(new Page<>(
                        pageNumber,
                        SystemConstants.MAX_PAGE_SIZE
                ));
        List<Blog> blogs=page.getRecords();
        fillBlogUsers(blogs);
        return Result.ok(blogs);
    }

    /**
     * 发布博客
     * Feed流作用于这里
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取当前登录用户
        UserDTO loginUser=UserHolder.getUser();

        if(loginUser==null){
            return Result.fail("当前尚未登录");
        }

        // 2. 检查博客内容
        if(blog==null
            ||blog.getShopId()==null
            ||StrUtil.isBlank(blog.getTitle())
            ||StrUtil.isBlank(blog.getContent())
            ||StrUtil.isBlank(blog.getImages())
        ){
            return Result.fail("博客内容不完整");
        }

        // 3. 后端强行覆盖不能由前端自行决定的字段 ,防止错误信息注入
        blog.setId(null);
        blog.setUserId(loginUser.getId());
        blog.setLiked(0);
        blog.setComments(0);

        // 4. 保存博客到MySQL
        boolean success=save(blog);

        if(!success){
            return Result.fail("博客保存失败");
        }

        /**
         * 5. Feed流
         *    查询当前作者的所有粉丝,查询条件为:
         *    user_id = 粉丝id;
         *    follow_user_id=被关注的达人id;
         */
        List<Follow> fans=followService.query()
                .eq("follow_user_id",loginUser.getId())
                .list();

        // 6. 

        long publishTime=System.currentTimeMillis();
        /**
         * 7. 将博客ID推送到每个粉丝的Feed收件箱
         *    用ZSet Redis,key是粉丝Id,members时粉丝关注的达人的博客,score是时间戳
         */
        for(Follow fan:fans){
            Long fanId=fan.getUserId();

            String feedKey=FEED_KEY+fanId;

            stringRedisTemplate.opsForZSet().add(
                    feedKey,
                    blog.getId().toString(),
                    publishTime
            );
        }
        // 数据库保存成功后id自增并回填至实体类,所以可以直接返回id
        return Result.ok(blog.getId());

    }

    @Override
    public Result likeBlog(Long id) {
        if(id==null||id<1){
            return Result.fail("博客不存在");
        }

        // 通过UserHolder获取当前登录用户信息(不依靠前端传入的用户信息,防止伪造)
        UserDTO userDTO =UserHolder.getUser();

        if(userDTO ==null){
            return Result.fail("当前尚未登录,请先登录");
        }
        Long userId=userDTO.getId();

        // 将blog的id拼接成key,userId作为member,查找是否存在对应关系来判断当前用户是否点赞过该博客
        String key=BLOG_LIKED_KEY+id;
        Double score=stringRedisTemplate
                .opsForZSet()
                .score(key,userId.toString());
        
        // score不存在说明当前用户和该博客无点赞关系,进行点赞操作
        if(score==null){
            boolean success=update()
                    .setSql("liked=liked+1")
                    .eq("id",id)
                    .update();
            if(!success){
                return Result.fail("点赞失败或博客不存在");
            }
            // 点赞,数据库更新成功则去Redis添加对应点赞关系
            stringRedisTemplate.opsForZSet().add(
                    key,
                    userId.toString(),
                    System.currentTimeMillis()
            );
            return Result.ok();
        }
        // score存在说明当前用户和该博客有点赞关系,进行取消点赞操作
        boolean success=update()
                .setSql("liked=liked-1")
                .eq("id",id)
                .gt("liked",0)
                .update();

        if(!success){
            return Result.fail("取消点赞失败");
        }
        stringRedisTemplate.opsForZSet().remove(key,userId.toString());
        return Result.ok();
    }  

    @Override
    public Result queryBlogLikes(Long id){
        if(id==null||id<1){
            return Result.fail("博客不存在");
        }
        String key=BLOG_LIKED_KEY+id;

        // score是时间戳 ,时间越大,点赞越晚
        Set<String> top5UserIds=stringRedisTemplate.opsForZSet()
                .reverseRange(key,0,4);

        if(top5UserIds==null||top5UserIds.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> userIds=top5UserIds.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());
        List<User> users=userService.listByIds(userIds);

        Map<Long,User> userMap=users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity()
                ));
        List<UserDTO> result = userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(user ->
                        BeanUtil.copyProperties(user, UserDTO.class)
                )
                .collect(Collectors.toList());

        return Result.ok(result);
    }
    /**
     * 私有内部辅助方法
     */
    // 核心方法:填充单个博客的User信息
    private void fillBlogUser(Blog blog){
        if(blog==null||blog.getUserId()==null){
            return;
        }
        User user=userService.getById(blog.getUserId());

        if(user==null){
            return;
        }
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
        isBlogLiked(blog);
    }
    // 核心方法:批量填充博客的User信息
    private void fillBlogUsers(List<Blog> blogs){  
        if(blogs==null||blogs.isEmpty()){
            return;
        }

        Set<Long> userIds = blogs.stream()
                .map(Blog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if(userIds.isEmpty()){
            return;
        }

        List<User> users=userService.listByIds(userIds);

        if(users==null||users.isEmpty()){
            return;
        }

        Map<Long,User> userMap=users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity()
                ));

        for(Blog blog:blogs){
            User user=userMap.get(blog.getUserId());
            if(user!=null){
                blog.setIcon(user.getIcon());
                blog.setName(user.getNickName());
                isBlogLiked(blog);
            }
        }
    }
    // 辅助方法,用于将前端传入的current合法化 ,非法输入默认设为1
    private int normalizeCurrent(Integer current){
        // 非法页码默认为1
        return current==null|| current<1
                ?1
                :current;
    }
    private void isBlogLiked(Blog blog){
        if(blog==null||blog.getId()==null){
            return;
        }

        UserDTO loginUser=UserHolder.getUser();

        // 因为当前路径无拦截,未登录用户访问直接显示未点赞,即isLike为false
        if(loginUser==null){
            blog.setIsLike(false);
            return;
        }

        String key=BLOG_LIKED_KEY+blog.getId();

        Double score=stringRedisTemplate
                .opsForZSet()
                .score(key,loginUser.getId().toString());
        blog.setIsLike(score!=null);
    }
}
