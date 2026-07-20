package com.hmdp;

import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

import java.time.ZoneId;
import java.util.List;

import static com.hmdp.utils.RedisConstants.*;

@SpringBootTest
@ActiveProfiles("test")
public class SeckillDataInitializerTest {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void loadSeckillData() {
        // 1. 从 MySQL 读取所有已创建的秒杀券活动。
        List<SeckillVoucher> vouchers = seckillVoucherService.list();

        // 2. 为每个活动重建 Redis 侧的秒杀基础数据。
        for (SeckillVoucher voucher : vouchers) {
            if (voucher.getVoucherId() == null
                    || voucher.getStock() == null
                    || voucher.getBeginTime() == null
                    || voucher.getEndTime() == null) {
                // 数据不完整的活动不能安全参与秒杀，跳过它而不写入不完整缓存。
                continue;
            }

            Long voucherId = voucher.getVoucherId();

            // 库存供 Lua 脚本用 DECR 判断；时间统一写入 Unix 毫秒时间戳。
            stringRedisTemplate.opsForValue().set(
                    SECKILL_STOCK_KEY + voucherId,
                    voucher.getStock().toString()
            );
            stringRedisTemplate.opsForValue().set(
                    SECKILL_BEGIN_KEY + voucherId,
                    String.valueOf(toEpochMillis(voucher.getBeginTime()))
            );
            stringRedisTemplate.opsForValue().set(
                    SECKILL_END_KEY + voucherId,
                    String.valueOf(toEpochMillis(voucher.getEndTime()))
            );

            // 初始化时必须删除旧资格，避免旧测试或历史活动残留造成重复下单限制。
            stringRedisTemplate.delete(SECKILL_ORDER_KEY + voucherId);
        }
    }

    private long toEpochMillis(java.time.LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}
