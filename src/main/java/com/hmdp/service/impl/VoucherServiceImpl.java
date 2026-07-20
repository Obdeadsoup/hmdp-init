package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.*;

import java.time.ZoneId;
import java.util.List;

@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 1. 保存优惠券主表
        save(voucher);
        // 2. 保存秒杀扩展表并初始化
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization(){
            @Override
            public void afterCommit(){
                cacheSeckillVoucher(seckillVoucher);
            }
        });
    }

    private void cacheSeckillVoucher(
            SeckillVoucher voucher
    ){
        Long id=voucher.getVoucherId();

        String stockKey = SECKILL_STOCK_KEY + id;
        String beginKey = SECKILL_BEGIN_KEY + id;
        String endKey = SECKILL_END_KEY + id;

        long beginMillis = voucher.getBeginTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        long endMillis = voucher.getEndTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        stringRedisTemplate.opsForValue().set(
                stockKey,
                voucher.getStock().toString()
        );        

        stringRedisTemplate.opsForValue().set(
                beginKey,
                String.valueOf(beginMillis)
        );
        stringRedisTemplate.opsForValue().set(
                endKey,
                String.valueOf(endMillis)
        );

        // 新活动到来前先清除旧活动的订单资格集合
        stringRedisTemplate.delete(
                SECKILL_ORDER_KEY + id
        );
    }
}
