package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService shopTypeService;
    @GetMapping("/list")
    public Result queryTypeList(){
        return shopTypeService.queryTypeList();
    }
    /*
    原本在Controller层实现的链式查询迁移到Service层,使Controller层变薄
    public Result queryTypeList() {
        List<ShopType> typeList = shopTypeService
                .query().orderByAsc("sort").list();
        return Result.ok(typeList);
    }
    */
    
    // 路径参数(单查,删除,单条更新,直接用/{参数})
    // /shop-type/{id} GET请求,查询商铺类型
    @GetMapping("/{id}")
    // @PathVariable注解,将路径参数"/{id}"绑定到方法的参数"id"上
    public Result getShopTypeById(@PathVariable Long id){
        ShopType shopType=shopTypeService.getShopTypeById(id);
        return Result.ok(shopType);
    }
}
