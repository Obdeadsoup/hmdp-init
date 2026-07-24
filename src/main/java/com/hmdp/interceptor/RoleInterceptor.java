package com.hmdp.interceptor;

import cn.hutool.json.JSONUtil;
import com.hmdp.annotation.RequireRole;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class RoleInterceptor implements HandlerInterceptor{

    @Resource
    private IUserService userService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    )throws Exception{
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        HandlerMethod handlerMethod= (HandlerMethod) handler;

        RequireRole requireRole=handlerMethod
                .getMethodAnnotation(RequireRole.class);

        if(requireRole==null){
            requireRole = handlerMethod
                    .getBeanType()
                    .getAnnotation(RequireRole.class);
        }
        if(requireRole==null){
            return true;
        }

        UserDTO loginUser=UserHolder.getUser();

        if(loginUser==null){
            return writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "用户未登录"
            );
        }

        User user=userService.getById(loginUser.getId());
        if(user==null){
            return writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "登录用户不存在"
            );
        }
        String requiredRole=requireRole.value();

        if(!requiredRole.equals(user.getRole())){
            return writeError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "当前用户无权限执行该操作"
            );
        }
        return true;
    }
    private boolean writeError(
            HttpServletResponse response,
            int status,
            String message
    )throws IOException{

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(
                "application/json;charset=UTF-8"
        );
        response.getWriter().write(JSONUtil.toJsonStr(Result.fail(message)));
        return false;
    }
}
