package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

public interface IFollowService extends IService<Follow> {

    /**
     * 关注或取关
     * @param followUserId 被关注/取关的用户id
     * @param isFollow 当前用户是否关注目标用户
     * @return
     */
    Result follow(Long followUserId,Boolean isFollow);
    /**
     * 当前用户是否关注目标用户
     * @param followUserId 目标用户
     * @return 
     */
    Result isFollow(Long followUserId);
    /**
     * 查询当前用户与目标用户的共同关注
     * @param otherUserId
     * @return
     */
    Result followCommons(Long otherUserId);
}
