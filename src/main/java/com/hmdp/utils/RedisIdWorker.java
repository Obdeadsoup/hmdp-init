package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
/**
 * Redis全局ID组件
 * RedisIdWorker
 */
@Component
public class RedisIdWorker {
    /**
     * 2022-01-01 00:00:00 UTC 取的是数据库中第一条数据的时间
     */
    private static final long BEGIN_TIMESTAMP=
            1640995200L;
    /**
     * 低32位保存每天的自增序列
     */
    private static final int COUNT_BITS = 32;
    private static final DateTimeFormatter
            DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy:MM:dd");
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix){
        long timestamp =
                Instant.now().getEpochSecond()
                        - BEGIN_TIMESTAMP;					  
        String date = LocalDate.now()
                .format(DATE_FORMATTER);

        String counterKey=
                RedisConstants.PROJECT_PREFIX
                        +"icr:"
                        +keyPrefix
                        +":"
                        +date;
        Long sequence =
                stringRedisTemplate.opsForValue()
                        .increment(counterKey);
        if (sequence == null) {
            throw new IllegalStateException(
                    "生成全局ID失败"
            );
        }
        return (timestamp << COUNT_BITS)
                | sequence;
    }
}
