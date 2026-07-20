package com.hmdp.utils;

public class RedisConstants {

    private RedisConstants(){}
    // 因为在本机Redis上使用,为防止多个项目的key冲突,统一加上"hmdp"的前缀
    public static final String PROJECT_PREFIX="hmdp:";

    /**
     * 手机验证码(String Redis)
     * hmdp:login:code:手机号
     * 验证码有效期 ,单位:分钟
     */
    public static final String LOGIN_CODE_KEY = PROJECT_PREFIX+"login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    /**
     * 登录用户(String Redis)
     * hmdp:login:token:token
     * 登录用户有效期 ,单位:分钟
     */
    public static final String LOGIN_USER_KEY = PROJECT_PREFIX+"login:token:";
    public static final Long LOGIN_USER_TTL = 60L;
    /**
     * 空值缓存过期时间，单位：分钟(String Redis)
     * 缓存key名字共用普通商户缓存
     * 用于防止缓存穿透
     */
    public static final Long CACHE_NULL_TTL = 2L;
    /**
     * 商铺缓存(Hash Redis)
     * hmdp:cache:shop:id
     * 商铺缓存有效期 ,单位:分钟
     */
    public static final String CACHE_SHOP_KEY = PROJECT_PREFIX+"cache:shop:";
    public static final Long CACHE_SHOP_TTL = 30L;
    /**
     * 商铺缓存Redis互斥锁前缀及过期时间,单位:秒
     * hmdp:lock:shop:id
     */
    public static final String LOCK_SHOP_KEY = PROJECT_PREFIX+"lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;
    /**
     * 博客点赞用户前缀
     * blog:liked:{id}
     */
    public static final String BLOG_LIKED_KEY = PROJECT_PREFIX+"blog:liked:";
    /**
     * 用户关注集合(Set)
     * key: hmdp:follows:{userId}
     * member: 被关注的用户id
     */
    public static final String FOLLOWS_KEY=PROJECT_PREFIX+"follows:";
    /**
     * 用户Feed收件箱(ZSet Redis类型Redis)
     * key: hmdp:feed:{userId}
     * member: 关注的博主的博客ID信息
     * score: 该博客发布时间戳
     */
    public static final String FEED_KEY = PROJECT_PREFIX+"feed:";
    /**
     * 用户每月签到Bitmap(String类型Redis)
     * key: hmdp:sign:{userId}:yyyyMM
     * value存当前月份每天签到情况
     */
    public static final String USER_SIGN_KEY = PROJECT_PREFIX+"sign:";
    /**
     * 用户每月活跃Bitmap(String Redis)
     * key: hmdp:active:{userId}:yyyyMM
     * value存当前月份每天活跃情况
     */
    public static final String USER_ACTIVE_KEY = PROJECT_PREFIX + "active:";
    /**
     * 用户历史活跃天数(String Redis)
     * key: hmdp:active:total:{userId}
     * value存累计天数
     */
    public static final String USER_ACTIVE_TOTAL_KEY = PROJECT_PREFIX + "active:total:";
    /**
     * 商铺类型所有商铺集合(Redis GEO)
     * key: hmdp:shop:geo:{typeId}
     * value中的member为该类型所有商铺的集合,附带point(x,y)的坐标信息
     */
    public static final String SHOP_GEO_KEY = PROJECT_PREFIX+"shop:geo:";

    // 下面这里就是秒杀业务涉及的Redis了
    /**
     * 秒杀券Redis库存
     * hmdp:seckill:stock:{voucherId}
     * value是库存量
     */
    public static final String SECKILL_STOCK_KEY =
            PROJECT_PREFIX + "seckill:stock:";
    /**
     * 已成功取得秒杀资格的用户集合
     * hmdp:seckill:order:{voucherId}
     * value是一个用户ID集合
     */
    public static final String SECKILL_ORDER_KEY =
            PROJECT_PREFIX + "seckill:order:";
    /**
     * 秒杀开始/结束时间
     * hmdp:seckill:begin/end:{voucherId}
     * value为时间
     */
    public static final String SECKILL_BEGIN_KEY =
            PROJECT_PREFIX + "seckill:begin:";
    public static final String SECKILL_END_KEY =
            PROJECT_PREFIX + "seckill:end:";
    /**
     * 秒杀订单消息队列
     * hmdp:stream:orders:{voucherId}
     */
    public static final String STREAM_ORDERS_KEY =
            PROJECT_PREFIX + "stream:orders";
    public static final String STREAM_ORDERS_GROUP =
            "g1";
    public static final String STREAM_ORDERS_CONSUMER =
            "c1";
    /**
     * 订单处理分布式锁
     * hmdp:lock:order:{voucherId}
     */
    public static final String LOCK_ORDER_KEY =
            PROJECT_PREFIX + "lock:order:";
    
}
