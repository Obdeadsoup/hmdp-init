package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;

public interface IBlogService extends IService<Blog> {
    // 查询热门博客
    Result queryHotBlog(Integer current);
    // 查询博客详情
    Result queryBlogById(Long id);
    // 查询我的博客
    Result queryMyBlog(Integer current);;
    // 查询指定用户博客主页
    Result queryBlogByUserId(Long userId,Integer current);
    // 发布博客
    Result saveBlog(Blog blog);
    // 点赞博客
    Result likeBlog(Long id);
    // 其实我也不知道这里为什么要加一个查询最近前5的用户
    Result queryBlogLikes(Long id);
}
