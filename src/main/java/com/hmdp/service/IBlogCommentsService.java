package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.CommentCreateDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.BlogComments;

public interface IBlogCommentsService extends IService<BlogComments> {
    
    Result createComment(CommentCreateDTO dto);
    
    Result queryRootComments(Long blogId,Integer current);

    Result queryReplies(Long parentId,Integer current);

    Result deleteComment(Long commentId);
}
