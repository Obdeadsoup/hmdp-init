package com.hmdp.controller;


import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;

@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService followService;

    /**
     * 
     * @param followUserId
     * @param isFollow
     * @return
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow(
            @PathVariable("id") Long followUserId,
            @PathVariable("isFollow") Boolean isFollow){

            return followService.follow(followUserId,isFollow);
    }
    /**
     * @param followUserId
     * @return
     */
    
    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable("id") Long followUserId){
        return followService.isFollow(followUserId);
    }
    /**
     * 
     * @param otherUserId
     * @return
     */
    @GetMapping("/commons/{id}")
    public Result followCommons(@PathVariable("id") Long otherUserId){
        return followService.followCommons(otherUserId);
    }


    
}
