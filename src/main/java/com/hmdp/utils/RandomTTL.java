package com.hmdp.utils;

import io.netty.util.internal.ThreadLocalRandom;

// 单独建一个工具类用来做随机过期时间,避免缓存雪崩
public class RandomTTL {
    public final static long randomTTL(long baseTTL){
        return baseTTL+ThreadLocalRandom.current().nextLong(1, 11);
    }
}
