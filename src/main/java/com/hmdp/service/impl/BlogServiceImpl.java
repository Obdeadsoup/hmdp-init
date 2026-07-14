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
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;


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

    @Override
    public Result saveBlog(Blog blog) {
        UserDTO loginUser=UserHolder.getUser();

        if(loginUser==null){
            return Result.fail("当前尚未登录");
        }

        if(blog==null
            ||blog.getShopId()==null
            ||StrUtil.isBlank(blog.getTitle())
            ||StrUtil.isBlank(blog.getContent())
            ||StrUtil.isBlank(blog.getImages())
        ){
            return Result.fail("博客内容不完整");
        }

        // 后端强行覆盖不能由前端自行决定的字段 ,防止错误信息注入
        blog.setId(null);
        blog.setUserId(loginUser.getId());
        blog.setLiked(0);
        blog.setComments(0);

        boolean success=save(blog);

        if(!success){
            return Result.fail("博客保存失败");
        }

        // 数据库保存成功后id自增并回填至实体类,所以可以直接返回id
        return Result.ok(blog.getId());

    }

    @Override
    public Result likeBlog(Long id) {return Result.fail("功能尚未实现");}  

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
}
