# 面试讲解笔记

## Redis 缓存问题

- 缓存穿透：查询不存在的数据反复打 DB。项目用空值缓存短 TTL 解决。
- 缓存击穿：热点 key 过期瞬间大量请求打 DB。项目用逻辑过期和互斥锁重建。
- 缓存雪崩：大量 key 同时失效。后续可用随机 TTL、预热、限流解决。

## 秒杀链路

秒杀入口先查活动时间，再用 Lua 在 Redis 中原子判断库存和一人一单，成功后预扣库存并写入订单队列。后台线程异步落库，DB 使用 `stock > 0` 条件扣减兜底超卖。

可追问点：

- Redis 预扣成功但 DB 失败如何回滚？
- processing 队列如何避免消息丢失？
- 为什么还需要 DB 唯一索引？

## 分布式锁

项目中有手写 Redis 锁和 Redisson 锁。手写锁使用 Lua 校验 owner 后删除；秒杀落库用 Redisson 用户维度锁，避免同一用户并发重复落单。

## Feed 流

发布博客时采用推模式，把 blogId 写入粉丝的 `feed:{userId}` ZSet。读取时用 `max + offset` 滚动分页，解决同一时间戳下重复或遗漏问题。

## 点赞排行榜

使用 `blog:liked:{blogId}` ZSet，member 是 userId，score 是点赞时间。可以快速查询前 5 个点赞用户并保持时间顺序。

## RAG

RAG 是“检索增强生成”：先从已有店铺、券、博客数据中检索证据，再把证据交给 LLM 生成回答。项目要求返回 references，无证据时拒答。

## Agent 工具调用

Agent 不直接查 DB，而是调用后端显式工具：

- 搜索候选店铺
- 获取店铺详情
- 获取优惠券
- 获取内容证据
- 排序候选店铺

这样边界清晰、可测试、可审计。

## 为什么 AI 不直接访问数据库

直接让模型查数据库会带来权限、SQL 注入、隐私泄露和不可控操作风险。正确做法是 Java 后端读取必要字段，脱敏和整理后再传给模型。

## 如何防止大模型胡编

- Prompt 中明确只能依据证据回答。
- Service 层返回 references。
- 检索不到证据时返回 `needMoreInfo=true`。
- 使用固定降级文案处理异常。

## 如何保证接口兼容

- 旧接口 URL、HTTP 方法、参数、`Result` 结构不改。
- AI 功能只新增 `/ai/**`。
- 使用 `ApiContractTest` 保护旧 Controller 映射。
- 前端实际调用清单记录在 `docs/frontend-backend-contract.md`。
