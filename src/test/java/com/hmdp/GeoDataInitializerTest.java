package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
@ActiveProfiles("test")
public class GeoDataInitializerTest {
    @Resource
    private IShopService shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void loadShopGeoData(){
        // 1. 获取所有商铺
        List<Shop> shops=shopService.list();

        /**
         * 2. 建立HashMap,key是shopTypeId,value是该商铺类型的所有商铺组成的List
         */
        Map<Long , List<Shop>> shopsByType=
                new HashMap<>();
        for(Shop shop:shops){
            if(shop.getTypeId()==null
                    ||shop.getX()==null
                    ||shop.getY()==null){
                continue;
            }

            Long typeId=shop.getTypeId();
            List<Shop> sameTypeShops=
                    shopsByType.get(typeId);
            // 这里如果是第一次遇到该类型的商铺,新建一个List
            if(sameTypeShops==null){
                sameTypeShops=new ArrayList<>();
                shopsByType.put(typeId,sameTypeShops);
            }
            sameTypeShops.add(shop);
            /**
             * 其实上面这一段我一开始没看懂,后面想起来Java里容器指向的是同一个位置
             * List<Shop> sameTypeShops=shopByType.get(typeId);
             * 上面这行代码已经把sameTypeShops指向了Map<typeId,List<Shop>>
             * sameTypeShops.add(shop)就相当是向Map<typeId,List<Shop>>插入一条shop
             */
        }
        /**
         * 3. 遍历每一种类型的商铺
         */
        for(Map.Entry<Long,List<Shop>> entry
                :shopsByType.entrySet()
        ){
            Long typeId=entry.getKey();
            List<Shop> sameTypeShops=entry.getValue();

            String key=SHOP_GEO_KEY+typeId;

            /**
             * 这一段其实我没理解逻辑,等会再看
             */
            stringRedisTemplate.delete(key);

            List<RedisGeoCommands.GeoLocation<String>>
                    locations=new ArrayList<>();

            /**
             * 4.
             */
            for(Shop shop:sameTypeShops){
                RedisGeoCommands.GeoLocation<String>
                        location=
                        new RedisGeoCommands.GeoLocation<>(
                                shop.getId().toString(),
                                new Point(
                                        shop.getX(),
                                        shop.getY()
                                )
                        );
                locations.add(location);
            }
            // 5.
            if(!locations.isEmpty()){
                stringRedisTemplate.opsForGeo()
                        .add(key,locations);
            }
        }
    }
}
