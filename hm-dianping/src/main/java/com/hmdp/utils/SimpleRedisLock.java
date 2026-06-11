package com.hmdp.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString() + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptSource(new ResourceScriptSource(new ClassPathResource("unlock.lua")));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 1. 生成锁的key
        String lockKey = KEY_PREFIX + name;
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 2. 尝试获取锁
        Boolean isSuccess = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isSuccess);
    }
    @Override
    public void unlock() {
        // 调用 Lua 脚本进行原子解锁
        String lockKey = KEY_PREFIX + name;
        String threadId = ID_PREFIX + Thread.currentThread().getId();

        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), threadId);
    }
//    @Override
//    public void unlock() {
//        // 1. 生成锁的key
//        String lockKey = KEY_PREFIX + name;
//        // 2. 解锁
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        String currentThreadId = stringRedisTemplate.opsForValue().get(lockKey);
//        if (currentThreadId != null && currentThreadId.equals(threadId)) {
//            stringRedisTemplate.delete(lockKey);
//        }
//    }
}
