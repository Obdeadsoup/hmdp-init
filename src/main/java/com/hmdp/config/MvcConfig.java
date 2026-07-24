package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import com.hmdp.interceptor.RoleInterceptor;
import com.hmdp.utils.UserActiveRecorder;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

// 这个@Configuration注解十分重要,该注解会在Spring启动时生成Bean并自动调用其中的拦截器;
@Configuration
public class MvcConfig implements WebMvcConfigurer{
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserActiveRecorder userActiveRecorder;
    @Resource
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        
        /**
         * 刷新token拦截器
         * 拦截所有请求,即便访问的是公开接口,只要携带了有效token也刷新登录有效期
         */
        registry.addInterceptor(
                new RefreshTokenInterceptor(
                        stringRedisTemplate,
                        userActiveRecorder
                )
            )
            .addPathPatterns("/**")
            // 这里的order很重要,先执行刷新token拦截器
            .order(0);

        /**
         * 登录校验拦截器
         * 除了公开接口,其他接口都要登录
         */    
        registry.addInterceptor(
                new LoginInterceptor()
            )
            .excludePathPatterns(
                "/user/code",
                "/user/login",
                "/user/info/**",

                "/shop-type/**",

                // 当前仅以下商铺、优惠券读取接口允许匿名访问
                "/shop/*",
                "/shop/of/type",
                "/shop/of/name",
                "/voucher/list/*",

                "/blog/hot",
                "/blog/of/user",
                "/blog/*",
                "/blog/likes/**"
            )
            .order(1);
        /**
         * 用户权限校验拦截器
         */
        registry.addInterceptor(
                roleInterceptor
            )
            .addPathPatterns("/**")
            .order(2);
    }
    
}
