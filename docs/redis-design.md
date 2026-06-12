# Redis 设计说明

## Redis 使用目标

本项目使用 Redis 承担四类职责：

1. 登录态和短期验证码。
2. 店铺、分类等热点数据缓存。
3. 秒杀库存、一人一单和异步订单队列。
4. 点赞、关注、Feed、GEO、签到、UV 等高频读写场景。

## Key 设计

| 场景 | Key | 类型 | 说明 |
| --- | --- | --- | --- |
| 验证码 | `login:code:{phone}` | String | 2 分钟 TTL |
| 登录态 | `login:token:{token}` | Hash | 保存 UserDTO 字段，访问时续期 |
| 店铺缓存 | `cache:shop:{id}` | String JSON | 店铺详情 |
| 店铺缓存锁 | `lock:shop:{id}` | String | 缓存重建互斥锁 |
| 店铺分类 | `shop:type:list` | String JSON | 分类列表 |
| 秒杀库存 | `seckill:stock:{voucherId}` | String | Redis 预扣库存 |
| 秒杀下单用户 | `seckill:order:{voucherId}` | Set | 一人一单标记 |
| 秒杀订单队列 | `queue:order` | List | 待处理订单 |
| 秒杀处理中队列 | `queue:order:processing` | List | 手动 ACK 保护 |
| 点赞 | `blog:liked:{blogId}` | ZSet | score 为点赞时间 |
| 关注 | `follow:{userId}` | Set | 用户关注列表 |
| Feed | `feed:{userId}` | ZSet | score 为发布时间 |
| GEO | `shop:geo:{typeId}` | GEO/ZSet | 按分类存储经纬度 |
| 签到 | `sign:{userId}:{yyyyMM}` | Bitmap | 月度签到 |
| UV | `uv:{yyyyMMdd}` | HyperLogLog | 每日 UV 近似去重 |

## 缓存策略

### 缓存穿透

店铺不存在时写入空值缓存，短 TTL，避免不存在 ID 反复打到数据库。

### 缓存击穿

热点店铺使用逻辑过期方案：

1. Redis 中保存数据和逻辑过期时间。
2. 未过期直接返回。
3. 已过期时先返回旧值。
4. 只有抢到互斥锁的线程异步重建缓存。

### 缓存雪崩

当前项目主要通过 TTL 和逻辑过期降低风险。后续可加入随机 TTL、热点 key 预热和限流。

## 一致性策略

- 店铺更新：先更新 MySQL，再删除缓存。
- 关注/点赞：MySQL 保存核心状态，Redis 用于高频查询和排序。
- 秒杀：Redis 先预扣，MySQL 后落库；失败时回滚 Redis 状态。

后续增强方向：

1. 缓存删除失败重试。
2. 事务提交后删除缓存。
3. 点赞和关注增加唯一索引或明细表做幂等兜底。
4. 秒杀队列迁移到 Redis Stream 消费者组。

## 面试讲法

可以把 Redis 设计概括为：

> Redis 在项目中既做缓存层，也做高并发削峰层和社交数据结构层。缓存侧解决穿透、击穿、雪崩；秒杀侧通过 Lua 原子预扣库存和一人一单；社交侧通过 ZSet、Set、Bitmap、HyperLogLog、GEO 分别支持点赞排行、共同关注、签到、UV 和附近商户。
