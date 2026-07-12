package com.hmdp.utils;

import com.hmdp.dto.UserDTO;

public class UserHolder {

    /**
     *  ThreadLocal是一个线程本地变量容器 ,每个线程只能看到自己保存的那份数据
     *  普通变量在一个请求链路里需要层层传参才能共享
     *  而ThreadLocal可以在同一个线程里共享数据,不需要传参
     * 业务链路里,请求通过UserHolder.getUser()获取用户信息,做到同一个请求的任何地方都能拿到信息;
    */ 
    private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

    // 保存当前线程对应的登录用户信息
    public static void saveUser(UserDTO user){
        tl.set(user);
    }

    public static UserDTO getUser(){
        return tl.get();
    }

    public static void removeUser(){
        tl.remove();
    }
}
