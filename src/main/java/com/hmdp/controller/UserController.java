package com.hmdp.controller;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.SetPasswordDTO;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm);
    }

    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        String token = request.getHeader("authorization");
        return userService.logout(token);
    }

    @PutMapping("/password")
    public Result setPassword(
        @RequestBody SetPasswordDTO form){
            return userService.setPassword(form);
        }

    @GetMapping("/me")
    public Result me(){
        //UserHolder是工具类,其内部方法均为静态方法,可通过类名直接调用;
        UserDTO user=UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        UserInfo info=userInfoService.getById(userId);
        
        if(info==null){
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return Result.ok(info);
    }
    // 这里的签到需要手动操作 ,后续我个人认为要改成每天只要用户登录过账号就记为签到
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }
    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }
    // 预留一个查询当月活跃天数的接口
    @GetMapping("/active-days")
    public Result activeDays(){
        return userService.activeDays();
    }
}
