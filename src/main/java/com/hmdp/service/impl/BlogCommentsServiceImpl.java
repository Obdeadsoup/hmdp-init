package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.CommentCreateDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.BlogComments;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.util.StrUtil;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {
    
    @Resource
    private IBlogService blogService;
    @Override
    @Transactional
    public Result createComment(CommentCreateDTO dto){
        // 1. 先对前端传来的DTO做检验
        if(dto.getBlogId()==null||dto.getBlogId()<1){
            return Result.fail("目标博客不存在");
        }
        if(StrUtil.isBlank(dto.getContent())){
            return Result.fail("内容不能为空");
        }
        String content=dto.getContent().trim();
        if(content.length()>255){
            return Result.fail("评论不能超过255个字符");
        }
        // 2. 获取当前登录用户并做检验
        UserDTO loginUser=UserHolder.getUser();
        if(loginUser==null){
            return Result.fail("请先登录");
        }
        // 3. 利用IBlogService注入,获取Blog
        Blog blog=blogService.getById(dto.getBlogId());
        if(blog==null){
            return Result.fail("博客不存在");
        }
        long parentId=dto.getParentId()==null
                ?0L
                :dto.getParentId();
        long anwserId=dto.getAnwserId()==null
                ?0L
                :dto.getAnwserId();
        // 4. 构造评论时要确保一级评论存在且被回复评论存在
        BlogComments comment=new BlogComments()
                .setAnswerId(anwserId)
                .setUserId(loginUser.getId())
                .setBlogId(dto.getBlogId())
                .setParentId(parentId)
                .setContent(content)
                .setLiked(0)
                .setStatus(0);
        boolean success=save(comment);
        if(!success){
            return Result.fail("评论发布失败");
        }

        /**
         * 5. 这里选择直接通过Mapper操作数据库而不是用java来更新
         *    这里IFNULL是为了兼容原先数据库comments字段可能为null;
         * 
         *    因为"@Transactional"默认只在抛出异常时回滚
         *    所以更新失败必须抛出运行时异常而不是Result.fail();
         *  
         */
        boolean blogUpdated=blogService.update()
                .setSql("comments = IFNULL(comments,0)+1")
                .eq("id",dto.getBlogId())
                .update();
        if(!blogUpdated){
            throw new IllegalStateException("当前评论更新失败");
        }
        // 6. save()成功后主键自增会自动回填到comment实体对象中
        return Result.ok(comment.getId());
    }

    @Override
    public Result queryRootComments(Long blogId, Integer current){
        return Result.ok();
    }

    @Override
    public Result queryReplies(Long parentId, Integer current){
        return Result.ok();
    }

    @Override
    public Result deleteComment(Long commentId){
        return Result.ok();
    }

    private int normalizeCurrent(Integer current){
        return current==null || current<1
                ?1
                :current;
    }
}
