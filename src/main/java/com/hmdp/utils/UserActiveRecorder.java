package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.hmdp.utils.RedisConstants.USER_ACTIVE_KEY;
import static com.hmdp.utils.RedisConstants.USER_ACTIVE_TOTAL_KEY;
/**
 * 用户活跃记录组件
 * UserActiveRecorder
 */
public class UserActiveRecorder {
    private static final ZoneId BUSINESS_ZONE=
            ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMM");
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void recordActiveDay(Long userId){
        if (userId == null) {
            return;
        }
        LocalDate today =
                LocalDate.now(BUSINESS_ZONE);
        
        // 
        String key=USER_ACTIVE_KEY
                + userId
                + ":"
                + today.format(MONTH_FORMATTER);
        

        int offset=today.getDayOfMonth()-1;

        Boolean oldValue=stringRedisTemplate
                .opsForValue()
                .setBit(key,offset,true);
        
        if(!Boolean.TRUE.equals(oldValue)){
            String activeTotalKey=USER_ACTIVE_TOTAL_KEY + userId;
            stringRedisTemplate
                    .opsForValue()
                    .increment(activeTotalKey);
        }
    }
    public long getTotalActiveDays(Long userId){
        String activeTotalKey=USER_ACTIVE_TOTAL_KEY + userId;
        String value = stringRedisTemplate
                .opsForValue()
                .get(activeTotalKey);

        if(value==null){
            return 0L;
        }
        try{
            return Long.parseLong(value);
        }catch(NumberFormatException exception){
            return 0L;
        }
    }
}
