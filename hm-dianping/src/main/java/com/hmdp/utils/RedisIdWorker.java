package com.hmdp.utils;

import com.hmdp.entity.Voucher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    private static final long BEGIN_TIMESTAMP=1640995200L;

    private static final long COUNT_BITS=32L;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
        //1.生成时间戳
        LocalDateTime now=LocalDateTime.now();
        long nowSecond=now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp=nowSecond-BEGIN_TIMESTAMP;

        //2.生成序列号
        //2,1获取当前日期 精确到天
        String date=now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        //2.2生成序列号
        long count= stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+date);
        //2.3拼接时间戳和序列号
        return timeStamp << COUNT_BITS | count;
    }
}
