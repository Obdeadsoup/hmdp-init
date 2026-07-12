package com.hmdp.utils;

public class RedisConstants {

    private RedisConstants(){}

    // 因为在本机Redis上使用,为防止多个项目的key冲突,统一加上"hmdp"的前缀
    public static final String PROJECT_PREFIX="hmdp:";

    /**
     * 手机验证码
     * hmdp:login:code:手机号
     * 验证码有效期 ,单位:分钟
     */
    public static final String LOGIN_CODE_KEY = PROJECT_PREFIX+"login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;

    /**
     * 登录用户
     * hmdp:login:token:token
     * 登录用户有效期 ,单位:分钟
     */
    public static final String LOGIN_USER_KEY = PROJECT_PREFIX+"login:token:";
    public static final Long LOGIN_USER_TTL = 60L;

    public static final Long CACHE_NULL_TTL = 2L;

    public static final Long CACHE_SHOP_TTL = 30L;
    public static final String CACHE_SHOP_KEY = PROJECT_PREFIX+"cache:shop:";

    public static final String LOCK_SHOP_KEY = PROJECT_PREFIX+"lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = PROJECT_PREFIX+"seckill:stock:";
    public static final String BLOG_LIKED_KEY = PROJECT_PREFIX+"blog:liked:";
    public static final String FEED_KEY = PROJECT_PREFIX+"feed:";
    public static final String SHOP_GEO_KEY = PROJECT_PREFIX+"shop:geo:";
    public static final String USER_SIGN_KEY = PROJECT_PREFIX+"sign:";
}
