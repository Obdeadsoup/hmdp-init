package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.entity.ShopType;

import com.hmdp.dto.Result;

public interface IShopTypeService extends IService<ShopType> {
    // 根据id查询商铺类型
    ShopType getShopTypeById(Long id);

    // 将原本Controller层的链式查询迁移至Service
    Result queryTypeList();
}
