package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.User;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;

public interface IUserService extends IService<User> {
    // 发送验证码
    Result sendCode(String phone);
    // 登录
    Result login(LoginFormDTO loginForm);
    // 登出
    Result logout(String token);
}
