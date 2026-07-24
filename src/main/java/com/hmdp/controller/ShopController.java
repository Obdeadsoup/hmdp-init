package com.hmdp.controller;
import com.hmdp.annotation.RequireRole;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCreateDTO;
import com.hmdp.dto.ShopUpdateDTO;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.UserRoles;

import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

import javax.annotation.Resource;

@RestController
@RequestMapping("/shop")
public class ShopController {
    @Resource
    public IShopService shopService;
    /**
     * 根据商铺ID查询商铺信息
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        // 业务实现已经迁移到Service层,这里直接调用即可
        return shopService.queryById(id);
    }
    /**
     * 新增商铺信息
    */

    @RequireRole(UserRoles.ADMIN)
    @PostMapping
    public Result saveShop(@Valid @RequestBody ShopCreateDTO request) {
        return shopService.saveShop(request);
    }
    /**
     * 更新商铺信息
    */
    @RequireRole(UserRoles.ADMIN)
    @PutMapping
    public Result updateShop(@Valid @RequestBody ShopUpdateDTO request) {
        return shopService.updateShop(request);
    }
    /**
     * 根据商铺类型分页查询商铺信息
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(
                    value = "current",
                    defaultValue = "1"
            ) Integer current,
            @RequestParam(
                    value="x",
                    required=false
            ) Double x,
            @RequestParam(
                    value="y",
                    required=false
            ) Double y
    ) {
        return shopService.queryShopByType(
                typeId,
                current,
                x,
                y
        );
    }
    /**
     * 根据商铺名称关键字分页查询商铺信息
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return Result.ok();
    }

    private Shop toShop(ShopUpdateDTO request) {
        return new Shop()
                .setId(request.getId())
                .setName(request.getName())
                .setTypeId(request.getTypeId())
                .setImages(request.getImages())
                .setArea(request.getArea())
                .setAddress(request.getAddress())
                .setX(request.getX())
                .setY(request.getY())
                .setAvgPrice(request.getAvgPrice())
                .setOpenHours(request.getOpenHours());
    }
}
