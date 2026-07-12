// 刷新 Redis Token 有效期
package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import cn.hutool.json.JSONUtil;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

public class RefreshTokenInterceptor implements HandlerInterceptor{
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(
        StringRedisTemplate stringRedisTemplate){
            this.stringRedisTemplate=stringRedisTemplate;
    }
    /**
     * Controller方法执行前调用
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler){

        // 通过前端传入的请求体得到头部token串
        String token=request.getHeader("authorization");
        // 该拦截器只做用户识别检验,不做登录拦截,所以如果token为空,直接放行
        if(StrUtil.isBlank(token)){
            return true;
        }

        // 与前缀拼接成key
        String tokenKey=LOGIN_USER_KEY+ token;

        // 根据tokenKey去Redis里找到对应的用户信息(这里信息有多个字段,采用的是Hash redis)
        Map<Object,Object> userMap=stringRedisTemplate
            .opsForHash().entries(tokenKey);

        // 若未在Redis中找到该tokenKey,证明未登录或已过期
        if(userMap.isEmpty()){
            return true;
        }

        // 把Redis中的Map转换为UserDTO对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(
            userMap,
            new UserDTO(),
            false
        );
        // 将用户信息存入ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 用户仍然活跃刷新tokenKey有效期
        stringRedisTemplate.expire(
            tokenKey,
            LOGIN_USER_TTL,
            TimeUnit.MINUTES
        );
        // 放行,进入Controller层
        return true;
    }     
    /**
     * Controller方法执行后调用
     * 这里主要是清理ThreadLocal中的用户信息,防止内存泄
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse reponse,
            Object handler,
            Exception ex
    ){
        UserHolder.removeUser();

    }
}
