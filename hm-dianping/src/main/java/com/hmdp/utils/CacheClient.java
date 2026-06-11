package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILDER_EXCUTER= Executors.newFixedThreadPool(10);

    public void set(String key, Object value, Long expireTime, TimeUnit timeUnit) {
        // 转换为字符串
        String valueStr = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key, valueStr, expireTime, timeUnit);
    }

    public void setLogicalExpire(String key, Object value, Long expireTime, TimeUnit timeUnit) {
        // 转换为字符串
        String valueStr = JSONUtil.toJsonStr(value);
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R,ID>R queryWithPassThrough(String keyPrefix, ID id, Class<R> clazz, Function<ID,R> dbFallback, Long expireTime, TimeUnit timeUnit) {
        // 从redis中查询
        String key = keyPrefix + id;
        String valueStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotEmpty(valueStr)) {
            // 如果redis中有，直接返回（需要反序列化，这里简化处理）
            R r = JSONUtil.toBean(valueStr, clazz);
            return r;
        }

        if (valueStr != null) {
            return null;
        }

        // 从数据库中查询
        R r = dbFallback.apply(id);
        if (r == null) {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", expireTime, timeUnit);
            return null;
        }

        // 将查询到的商铺信息写入redis（需要序列化）
        this.set(key, r, expireTime, timeUnit);
        return r;
    }

    public <R,ID>R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> clazz, Function<ID,R> dbFallback, Long expireTime, TimeUnit timeUnit) {
        // 从redis中查询
        String key = keyPrefix + id;
        String valueStr = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(valueStr)) {
            // 如果redis中为空，说明数据库中也没有，返回null
            return null;
        }

        // 4.1.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(valueStr, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) (redisData.getData()), clazz);
        LocalDateTime expireTimenew = redisData.getExpireTime();
        //5.判断是否过期
        //5.1.未过期，直接返回店铺信息
        if (expireTimenew.isAfter(LocalDateTime.now())) {
            return r;
        }

        //5.2.已过期，需要缓存重建
        //6.缓存重速
        //6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        //6.2.判断是否获取锁成功
        if (isLock) {
            //6.3.成功，开启独立线程，实现缓存重建
            CACHE_REBUILDER_EXCUTER.submit(() -> {
                try {
                    //查询数据库
                    R r1 = dbFallback.apply(id);
                    //写入redis
                    this.setLogicalExpire(key, r1, expireTime, timeUnit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
            return r;
        }
        //6.4.返回过期的商铺信息
        return r;
    }

    //互斥锁
    private boolean tryLock(String key) {
        // 尝试获取锁
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        // 返回是否获取到锁
        return BooleanUtil.isTrue(lock);
    }

    // 释放锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

}
