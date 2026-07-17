package com.hmdp.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {
    /**
     * 所属博客
     */
    private Long blogId;
    /**
     * 所属一级评论ID
     */
    private Long parentId;
    /**
     * 所回复的评论ID
     */
    private Long anwserId;
    /**
     * 内容
     */
    private String content;
}
