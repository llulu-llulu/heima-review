package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.SECKILL_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    // 主队列保存待处理订单；processing 队列保存已取出但还未确认完成的订单。
    private static final String ORDER_QUEUE = "queue:order";
    private static final String PROCESSING_QUEUE = "queue:order:processing";
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final DefaultRedisScript<Long> RESTORE_STOCK_SCRIPT;
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);

        // 数据库落单失败时回滚 Redis 状态：只有成功移除用户下单标记时才补回库存，保证幂等。
        RESTORE_STOCK_SCRIPT = new DefaultRedisScript<>();
        RESTORE_STOCK_SCRIPT.setScriptText(
                "local stockKey = ARGV[1]\n" +
                "local orderKey = ARGV[2]\n" +
                "local userId = ARGV[3]\n" +
                "if redis.call('srem', orderKey, userId) == 1 then\n" +
                "    return redis.call('incr', stockKey)\n" +
                "end\n" +
                "return 0"
        );
        RESTORE_STOCK_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Lazy
    @Resource
    private IVoucherOrderService proxy;

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        if (seckillVoucher == null) {
            return Result.fail("优惠券不存在");
        }
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }

        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );
        int r = result == null ? -1 : result.intValue();
        if (r == 1) {
            return Result.fail("库存不足");
        }
        if (r == 2) {
            return Result.fail("不允许重复下单");
        }
        if (r != 0) {
            return Result.fail("下单失败");
        }

        return Result.ok(orderId);
    }

    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            recoverProcessingQueue();
            while (true) {
                String message = null;
                try {
                    // BRPOPLPUSH 原子地把订单从主队列移入 processing 队列，避免消费线程宕机造成消息丢失。
                    message = stringRedisTemplate.opsForList()
                            .rightPopAndLeftPush(ORDER_QUEUE, PROCESSING_QUEUE, Duration.ofSeconds(2));
                    if (message == null) {
                        continue;
                    }
                    VoucherOrder voucherOrder = JSONUtil.toBean(message, VoucherOrder.class);
                    handleVoucherOrder(voucherOrder);
                    acknowledge(message);
                } catch (Exception e) {
                    log.error("处理 Redis List 秒杀订单异常，message={}", message, e);
                    sleep(50);
                }
            }
        }
    }

    private void recoverProcessingQueue() {
        while (true) {
            // 应用重启时把未确认完成的订单搬回主队列，后续消费线程会重新处理。
            String message = stringRedisTemplate.opsForList().rightPopAndLeftPush(PROCESSING_QUEUE, ORDER_QUEUE);
            if (message == null) {
                return;
            }
        }
    }

    private void acknowledge(String message) {
        // 只有数据库订单创建成功后才从 processing 队列删除，形成手动 ack。
        stringRedisTemplate.opsForList().remove(PROCESSING_QUEUE, 1, message);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) throws InterruptedException {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = false;
        try {
            isLock = lock.tryLock(1, 10, TimeUnit.SECONDS);
            if (!isLock) {
                throw new IllegalStateException("failed to acquire order lock");
            }
            Result result = proxy.createVoucherOrder(voucherOrder);
            if (!Boolean.TRUE.equals(result.getSuccess())) {
                restoreRedisState(voucherOrder);
            }
        } finally {
            if (isLock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void restoreRedisState(VoucherOrder voucherOrder) {
        stringRedisTemplate.execute(
                RESTORE_STOCK_SCRIPT,
                Collections.emptyList(),
                SECKILL_STOCK_KEY + voucherOrder.getVoucherId(),
                SECKILL_ORDER_KEY + voucherOrder.getVoucherId(),
                voucherOrder.getUserId().toString()
        );
    }

    @Override
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(redisIdWorker.nextId("order"));
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        return createVoucherOrder(voucherOrder);
    }

    @Override
    @Transactional
    public Result createVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.ok(voucherOrder.getId());
        }

        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足");
        }

        save(voucherOrder);
        return Result.ok(voucherOrder.getId());
    }
}
