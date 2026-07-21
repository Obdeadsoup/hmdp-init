package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class VoucherOrderTxServiceImpl {
    
    @Resource
    private VoucherOrderMapper voucherOrderMapper;
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Transactional
    public void createVoucherOrder(VoucherOrder order) {
        Long userId=order.getUserId();
        Long voucherId=order.getVoucherId();

        // 1. 幂等检查
        Long count =voucherOrderMapper.selectCount(
                new LambdaQueryWrapper<VoucherOrder>()
                        .eq(VoucherOrder::getUserId,userId)
                        .eq(VoucherOrder::getVoucherId,voucherId)
        );
        if(count!=null&&count>0){
            return;
        }
        // 2. 扣减数据库库存
        boolean success=seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id",voucherId)
                .gt("stock",0)
                .update();
        if(!success){
            throw new IllegalStateException("数据库库存不足或库存不一致");
        }

        // 3. 创建数据库订单
        int rows=voucherOrderMapper.insert(order);

        if(rows!=1){
            throw new IllegalStateException("创建订单失败");
        }
    }
}
