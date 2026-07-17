package com.hmdp.controller;


import javax.annotation.Resource;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.dto.CommentCreateDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.IBlogCommentsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {
    @Resource 
    private IBlogCommentsService blogCommentsService;

    /**
     * 发布评论或回复,需要登录
     * POST /blog-comments
     */
    @PostMapping
    public Result createComment(@RequestBody CommentCreateDTO dto){
        return blogCommentsService.createComment(dto);
    }

    /**
     * 分页查询某博客的一级评论
     * GET /blog-comments/blog/{blogId}?current=1
     * @param id 传入博客id
     */
    @GetMapping("/blog/{blogId}")
    public Result queryRootComments(
            @PathVariable("blogId") Long blogId,
            @RequestParam(
                    value="current",
                    defaultValue="1"
            )Integer current
    ){
        return blogCommentsService.queryRootComments(
                blogId,
                current
        );
    }
    /**
     * 分页查询某条一级评论的回复
     * GET /blog-comments/{parentId}/replies?current=1
     * @param id
     * @return
     */
    @GetMapping("/{parentId}/replies")
    public Result queryReplies(
            @PathVariable Long parentId,
            @RequestParam(
                    value="current",
                    defaultValue="1"
            )Integer current
    ){
        return blogCommentsService.queryReplies(parentId , current);
    }

    /**
     * 删除自己的某条评论
     * DELETE /blog-comments/{id}
     * @param id
     * @return
     */
    @PutMapping("/{id}")
    public Result deleteComment(@PathVariable("id") Long id){
        return blogCommentsService.deleteComment(id);
    }
    
}
