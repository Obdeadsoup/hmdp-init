package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.Shop;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCreateDTO;
import com.hmdp.dto.ShopUpdateDTO;

public interface IShopService extends IService<Shop> {
    
    /**
     * 根据ID查询商铺信息且优先使用Redis
     */
    Result queryById(Long id);
    /**
     * 更新商铺信息
     */
    Result updateShop(ShopUpdateDTO request);
    /**
     * 根据商铺类型按距离远近查询商铺
     * @param typeId 商铺类型
     * @param current 当前页数
     * @param x 用户当前位置经度
     * @param y 用户当前位置纬度
     */
    Result queryShopByType(
            Integer typeId,
            Integer current,
            Double x,
            Double y
    );
    Result saveShop(ShopCreateDTO dto);
}
