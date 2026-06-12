# 秒杀链路设计说明

## 目标

秒杀链路需要解决：

- 高并发请求削峰
- 库存不能超卖
- 同一用户一人一单
- 下单接口快速返回
- Redis 与 MySQL 最终一致

## 当前链路

1. 用户请求 `POST /voucher-order/seckill/{id}`。
2. 后端查询秒杀券，校验开始和结束时间。
3. 生成订单 ID。
4. 执行 `seckill.lua`。
5. Lua 在 Redis 中原子完成：
   - 判断 `seckill:stock:{voucherId}` 是否充足。
   - 判断 `seckill:order:{voucherId}` 是否已有用户。
   - 扣减 Redis 库存。
   - 写入用户下单 Set。
   - 将订单消息写入 `queue:order`。
6. 接口返回订单 ID。
7. 后台线程使用 `BRPOPLPUSH` 把消息从 `queue:order` 移入 `queue:order:processing`。
8. 消费者按用户维度加 Redisson 锁。
9. 事务内检查订单、扣减 MySQL 库存、保存订单。
10. 成功后从 processing 队列 ACK 删除消息。

## 防超卖设计

### Redis 层

Lua 脚本保证库存判断和扣减原子执行，避免并发请求同时看到相同库存。

### MySQL 层

落库时使用：

```sql
stock = stock - 1
where voucher_id = ? and stock > 0
```

即使 Redis 层出现异常，DB 条件扣减仍能兜底防超卖。

## 一人一单设计

### Redis 层

`seckill:order:{voucherId}` Set 记录已下单用户，Lua 中使用 `SISMEMBER` 判断是否重复。

### Java 层

异步落库时按 `lock:order:{userId}` 加 Redisson 锁，降低同一用户并发落库风险。

### DB 层后续增强

建议新增唯一索引：

```sql
alter table tb_voucher_order
add unique key uk_user_voucher (user_id, voucher_id);
```

## 当前已修复的问题

- `RedisIdWorker` 已改为使用 epoch seconds，避免订单 ID 时间戳高位错误。
- 接口契约测试已覆盖 `POST /voucher-order/seckill/{id}` 路径和 HTTP 方法。

## 仍需增强

1. `createVoucherOrder` 抛异常时也要回滚 Redis 预扣状态。
2. processing 队列需要运行期超时扫描，不能只在启动时恢复。
3. 需要重试次数、死信队列和告警。
4. 需要唯一索引兜底一人一单。
5. 后续可迁移到 Redis Stream 消费者组。

## 面试讲法

可以用一句话概括：

> 秒杀入口不直接打 MySQL，而是用 Redis Lua 原子完成库存预扣和一人一单，订单写入 Redis 队列后异步落库；数据库侧再通过条件扣减和唯一索引做最终兜底，从而同时解决削峰、超卖和重复下单问题。
