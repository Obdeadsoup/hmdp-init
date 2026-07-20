package com.hmdp.service.impl;

import com.hmdp.utils.SystemConstants;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RandomTTL;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
@Service
public class ShopServiceImpl 
        extends ServiceImpl<ShopMapper, Shop> 
        implements IShopService {
    //Redis注入 
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 重写IShopService接口中的queryById方法,实现Redis缓存逻辑
     */
    @Override
    public Result queryById(Long id){
        if(id==null||id<1){
            return Result.fail("商铺ID不合法");
        }
        /* 原业务逻辑
        // 拼接成key去Redis中查找商铺
        String key=CACHE_SHOP_KEY+id;
        String shopJson=
                stringRedisTemplate.opsForValue().get(key);
        // 如果在Redis中找到了,直接返回Shop实体
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop=JSONUtil.toBean(shopJson,Shop.class);
            return Result.ok(shop);
        }
        // 上既不是Blank又不是NULL,为空值缓存
        if(shopJson!=null){
            return Result.fail("商铺不存在");
        }
        
        // 没找到,去数据库中查找(这里调用的是MyBatis-Plus提供的方法)
        Shop shop=getById(id);
        // 按ID查找到的结果为空直接返回

        // !!!
        // 这里加一条逻辑 ,若MySQL中查找结果为空,写入空值缓存
        if(shop==null){
            stringRedisTemplate.opsForValue().set(
                    key,
                    "",
                    CACHE_NULL_TTL,
                    TimeUnit.MINUTES
            );
            return Result.fail("商铺不存在");
        }
        // 查到结果,将MySQL中查到的Shop对象转为JSON存入Redis
        stringRedisTemplate.opsForValue().set(
                key,
                JSONUtil.toJsonStr(shop),
                RandomTTL.randomTTL(CACHE_SHOP_TTL),
                TimeUnit.MINUTES
        );
        */
        Shop shop=queryWithMutex(id);

        if(shop==null){
            return Result.fail("商铺不存在");
        }

        return Result.ok(shop);
    }

    /**
     * 重写IShopService接口中的updateShop方法,实现更新商铺信息时删除Redis缓存
     */
    @Override
    public Result updateShop(Shop shop){
        if(shop==null||shop.getId()==null){
            return Result.fail("商铺ID不合法");
        }

        /**
         * 这里调用的是MyBatis-Plus Service层提供的方法
         * 成功更新时success==true,失败则为false 
         * 并且MyBatis-Plus默认更新非空字段 ,前端未传入的字段不会被修改成null
         */
        boolean success=updateById(shop);
        if(!success){
            return Result.fail("商铺更新失败");
        }

        // 根据传入的商铺id删除对应缓存 ,这里的getId()是Shop实体类@Data注解自带的方法
        stringRedisTemplate.delete(
                CACHE_SHOP_KEY+shop.getId()
        );
        return Result.ok();
    }
    /**
     * GEO Redis附近商铺查询业务(位置不完整时为普通查询)
     */
    @Override
    public Result queryShopByType( 
            Integer typeId,
            Integer current,
            Double x,
            Double y        
    ){
        if(typeId==null||typeId<1){
            return Result.fail("商铺类型不合法");
        }

        int pageNumber= current==null||current<1
                ?1
                :current;

        // 如果位置信息不完整直接返回默认结果(不按距离排序)
        if(x==null||y==null){
            Page<Shop> page=query()
                    .eq("type_id",typeId)
                    .page(new Page<>(
                            pageNumber,
                            SystemConstants.DEFAULT_PAGE_SIZE
                    ));
            return Result.ok(page.getRecords());
        }

        if(x< -180||x>180 ||y< -90 || y>90){
            return Result.fail("位置信息不合法");
        }

        /**
         * 核心代码
         * 每页5条
         * 第一页:from=0,end=5
         * 第二页:from=5,end=10
         * 第三页:from=10,end=15
         */
        int pageSize=SystemConstants.DEFAULT_PAGE_SIZE;
        int from = (pageNumber - 1) * pageSize;
        int end = pageNumber * pageSize;    

        String key=SHOP_GEO_KEY+typeId;

        GeoResults<RedisGeoCommands.GeoLocation<String>>
                results=
                stringRedisTemplate.opsForGeo().search(
                        key,
                        GeoReference.fromCoordinate(x,y),
                        new Distance(
                                5,
                                Metrics.KILOMETERS
                        ),
                        RedisGeoCommands
                            .GeoSearchCommandArgs
                            .newGeoSearchArgs()
                            // 返回每家商铺与用户的距离
                            .includeDistance()
                            // 按距离从近到远
                            .sortAscending()
                            // 先取前end条
                            .limit(end)
                );
        
        if(results==null){
            return Result.ok(Collections.emptyList());
        }

        List<GeoResult
                <RedisGeoCommands.GeoLocation<String>>>
                content=results.getContent();
        
        if(content.size()<=from){
            return Result.ok(Collections.emptyList());
        }

        List<Long> shopIds=new ArrayList<>();
        Map<Long , Double> distanceMap=
                new HashMap<>();

        for(int i=from;i<content.size();i++){
            GeoResult<
                    RedisGeoCommands.GeoLocation<String>>
                    geoResult=content.get(i);

            String shopIdValue=
                    geoResult.getContent().getName();

            Long shopId=
                    Long.valueOf(shopIdValue);

            double distance=
                    geoResult.getDistance().getValue();

            shopIds.add(shopId);
            distanceMap.put(shopId,distance);
        }

        List<Shop> queriedShops=listByIds(shopIds);

        Map<Long,Shop> shopMap=new HashMap<>();

        for(Shop shop:queriedShops){
            shopMap.put(shop.getId(),shop);
        }

        List<Shop> result=new ArrayList<>();
        for(Long shopId:shopIds){
            Shop shop = shopMap.get(shopId);
            if(shop==null){continue;}
            shop.setDistance(distanceMap.get(shopId));
            result.add(shop);
        }
        return Result.ok(result);
    }
    /** 
     * Redis互斥锁核心方法
     */ 
    private Shop queryWithMutex(Long id){
        /**
         * cacheKey是用来查找商铺缓存,lockKey是用来获取互斥锁的
         * lockKey需要根据id拼接,因为不同商铺的缓存是独立的,锁也应该是独立的
         */ 
        String cacheKey=CACHE_SHOP_KEY+id;
        String lockKey=LOCK_SHOP_KEY+id;

        while(true){
            // 先根据ID key去Redis中查商铺
            String shopJson=stringRedisTemplate.opsForValue().get(cacheKey);

            // 正常命中缓存,返回结果
            if(StrUtil.isNotBlank(shopJson)){
                return JSONUtil.toBean(shopJson,Shop.class);
            }

            // (缓存穿透部分会有存入空缓存)命中空缓存
            if(shopJson !=null){
                return null;
            }

            // 没有命中缓存 ,尝试获取互斥锁
            boolean locked=tryLock(lockKey);

            if(!locked){
                // 获取锁失败,调用辅助方法休眠一段时间后继续尝试
                sleepBriefly();
                continue;
            }

            try{
                /**
                 * 获取锁后需要再次检查缓存是否存在
                 * 因为等待锁的过程中,可能有别的线程已经完成缓存重建
                 * 故这里先查缓存避免重复查库
                 */
                shopJson=stringRedisTemplate.opsForValue().get(cacheKey);
                if(StrUtil.isNotBlank(shopJson)){
                    return JSONUtil.toBean(shopJson,Shop.class);
                }
                if(shopJson !=null){
                    return null;
                }

                // 确认缓存没有后,查数据库
                Shop shop=getById(id);

                if(shop==null){
                    // 数据库中没有 ,写入空缓存
                    stringRedisTemplate.opsForValue().set(
                            cacheKey,
                            "",
                            CACHE_NULL_TTL,
                            TimeUnit.MINUTES
                    );
                    return null;
                }
                // 数据库中有 ,写入缓存
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        JSONUtil.toJsonStr(shop),
                        RandomTTL.randomTTL(CACHE_SHOP_TTL),
                        TimeUnit.MINUTES
                );  
                return shop;
            }finally{
                // 无论是否异常都要释放锁,防止锁一直被占用
                unlock(lockKey);
            }
        }
    }

    /**
     * Redis互斥锁辅助方法
     */
    private boolean tryLock(String key){
        // 如果该锁不存在就创建(返回true,获取锁),并设置过期时间 ,成功返回true,失败返回false
        // 这里使用Boolean包装类是因为StringRedisTemplate.opsForValue().setIfAbsent()返回的是Boolean对象,而不是boolean基本类型
        Boolean success=stringRedisTemplate.opsForValue()
                .setIfAbsent(
                        key,
                        "1",
                        LOCK_SHOP_TTL,
                        TimeUnit.SECONDS
                );
        return Boolean.TRUE.equals(success);
    }

    private void unlock(String key){
        // 释放锁
        stringRedisTemplate.delete(key);
    }

    private void sleepBriefly(){
        try{
            TimeUnit.MILLISECONDS.sleep(50);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "等待缓存重建时线程被打断",
                    e
            );
        }
    }
}
