package com.hmdp.controller;


import com.hmdp.annotation.RequireRole;
import com.hmdp.dto.Result;
import com.hmdp.dto.SeckillVoucherCreateDTO;
import com.hmdp.dto.VoucherCreateDTO;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.UserRoles;

import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 新增普通券
     * @param request 优惠券信息
     * @return 优惠券id
     */
    @RequireRole(UserRoles.ADMIN)
    @PostMapping
    public Result addVoucher(@Valid @RequestBody VoucherCreateDTO request) {
        return voucherService.addVoucher(request);
    }

    /**
     * 新增秒杀券
     * @param request 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @RequireRole(UserRoles.ADMIN)
    @PostMapping("/seckill")
    public Result addSeckillVoucher(
            @Valid @RequestBody SeckillVoucherCreateDTO request
    ) {
        return voucherService.addSeckillVoucher(request);
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
