package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.User;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.SetPasswordDTO;

public interface IUserService extends IService<User> {
    // 发送验证码
    Result sendCode(String phone);
    // 登录
    Result login(LoginFormDTO loginForm);
    // 登出
    Result logout(String token);
    // 设置账号密码
    Result setPassword(SetPasswordDTO form);
    // 每日签到
    Result sign();
    // 统计本月截至今天的签到天数
    Result signCount();
    // 统计使用天数
    Result activeDays();
}
