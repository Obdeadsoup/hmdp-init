package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
@Service
public class ShopServiceImpl 
        extends ServiceImpl<ShopMapper, Shop> 
        implements IShopService {
    // 要用到Redis,先注入
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id){
        if(id==null||id<1){
            return Result.fail("商铺ID不合法");
        }
        // 拼接成key去Redis中查找商铺
        String key=CACHE_SHOP_KEY+id;
        String shopJson=
                stringRedisTemplate.opsForValue().get(key);
        // 如果在Redis中找到了,直接返回Shop实体
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop=JSONUtil.toBean(shopJson,Shop.class);
            return Result.ok(shop);
        }

        // 没找到,去数据库中查找(这里调用的是MyBatis-Plus提供的方法)
        Shop shop=getById(id);
        // 按ID查找到的结果为空直接返回
        if(shop==null){
            return Result.fail("商铺不存在");

        }
        // 查到结果,将MySQL中查到的Shop对象转为JSON存入Redis
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(shop),
                CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );

        return Result.ok(shop);
    }
}
