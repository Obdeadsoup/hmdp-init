package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;

import cn.hutool.core.bean.BeanUtil;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.connection.stream.ReadOffset;

import static com.hmdp.utils.RedisConstants.*;
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private VoucherOrderTxServiceImpl voucherOrderTxService;

    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run(){
            // 项目重启后先处理Pending中遗留消息
            handlePendingList();
            while(!Thread.currentThread().isInterrupted())
            {
                try{
                    List<MapRecord<String,Object,Object>>
                            records=stringRedisTemplate.opsForStream()
                                    .read(
                                    Consumer.from(
                                    STREAM_ORDERS_GROUP,
                                    STREAM_ORDERS_CONSUMER
                                    ),
                                    StreamReadOptions
                                    .empty()
                                    .count(1)
                                    .block(Duration.ofSeconds(2)),
                                    StreamOffset.create(
                                    STREAM_ORDERS_KEY,
                                    ReadOffset.lastConsumed()
                                    )
                            );

                    if (records == null || records.isEmpty()) {
                        continue; 
                    }
                    MapRecord<String,Object,Object> record=records.get(0);
                    
                    VoucherOrder order=BeanUtil.fillBeanWithMap(
                            record.getValue(),
                            new VoucherOrder(),
                            true
                    );
                    
                    handleVoucherOrder(order);
                    
                    stringRedisTemplate.opsForStream()
                            .acknowledge( 
                                    STREAM_ORDERS_KEY, 
                                    STREAM_ORDERS_GROUP, 
                                    record.getId() );
                }catch(Exception e){
                        log.error("处理秒杀订单消息时异常",e);
                        handlePendingList();
                }
            }
        }
    }

    // 加载resources目录下的Lua脚本
    private static final DefaultRedisScript<Long>
            SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT =
                new DefaultRedisScript<>();

        SECKILL_SCRIPT.setLocation(
                new ClassPathResource("seckill.lua")
        );

        SECKILL_SCRIPT.setResultType(Long.class);
    }
    // 创建单线程消费者
    private static final ExecutorService 
                SECKILL_ORDER_EXECUTOR=
                Executors.newSingleThreadExecutor();

    @Override
    public Result seckillVoucher(Long voucherId){
        if(voucherId==null||voucherId<1){
            return Result.fail("优惠券ID不合法");
        }
        UserDTO loginUser = UserHolder.getUser();

        if (loginUser == null) {
            return Result.fail("用户未登录");
        }

        Long userId = loginUser.getId();

        /**
         * 先生成订单ID
         * Lua成功后该ID会被写入Stream并作为数据库的最终订单ID
        */
        long orderId=redisIdWorker.nextId("order"); 

        List<String>keys=Arrays.asList(
                SECKILL_STOCK_KEY+voucherId,
                SECKILL_ORDER_KEY+voucherId,
                SECKILL_BEGIN_KEY+voucherId,
                SECKILL_END_KEY+voucherId,
                STREAM_ORDERS_KEY
        );

        // 运行Lua脚本 ,指定脚本 ,指定keys参数 ,指定普通业务参数
        Long result=
                stringRedisTemplate.execute(
                        SECKILL_SCRIPT,
                        keys,
                        userId.toString(),
                        voucherId.toString(),
                        String.valueOf(orderId),
                        String.valueOf(
                                System.currentTimeMillis()
                        )
                );

        int code =result.intValue();
        switch(code){
            case 0:
                return Result.ok(orderId);
            case 1:
                return Result.fail("库存不足");
            case 2:
                return Result.fail("不能重复下单");
            case 3:
                return Result.fail("秒杀未开始");
            case 4:
                return Result.fail("秒杀已结束");
            case 5:
                return Result.fail("秒杀活动不存在");
            default:
                return Result.fail("未知错误");
        }
    }

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(
                new VoucherOrderHandler()
        );
    }
    @PreDestroy
    private void shutdown(){
        SECKILL_ORDER_EXECUTOR.shutdownNow();
    }
    private void handleVoucherOrder(VoucherOrder order){
        if (order == null
            || order.getId() == null
            || order.getUserId() == null
            || order.getVoucherId() == null) {
                throw new IllegalArgumentException(
                        "秒杀订单消息不完整"
                );
        }
        voucherOrderTxService.createVoucherOrder(order);
    }
    
    private void handlePendingList(){
        while (!Thread.currentThread()
                .isInterrupted()) {
            try {
                /*
                * ReadOffset.from("0")
                * 表示读取当前消费者未确认的Pending消息。
                */
                List<MapRecord<
                        String,
                        Object,
                        Object>>
                        records =
                        stringRedisTemplate
                                .opsForStream()
                                .read(
                                Consumer.from(
                                        STREAM_ORDERS_GROUP,
                                        STREAM_ORDERS_CONSUMER
                                ),
                                StreamReadOptions
                                        .empty()
                                        .count(1),
                                StreamOffset.create(
                                        STREAM_ORDERS_KEY,
                                        ReadOffset.from("0")
                                )
                                );
                // Pending已经处理完
                if (records == null
                        || records.isEmpty()) {
                        break;
                }
                MapRecord<
                        String,
                        Object,
                        Object>
                        record = records.get(0);

                VoucherOrder order =
                        BeanUtil.fillBeanWithMap(
                                record.getValue(),
                                new VoucherOrder(),
                                true
                        );

                handleVoucherOrder(order);

                stringRedisTemplate
                        .opsForStream()
                        .acknowledge(
                                STREAM_ORDERS_KEY,
                                STREAM_ORDERS_GROUP,
                                record.getId()
                        );

                } catch (Exception e) {
                log.error(
                        "处理Pending订单消息异常",
                        e
                );

                /*
                * 避免数据库持续故障时疯狂空转。
                */
                try {
                        TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        return;
                }
            }
        }
    }


}
