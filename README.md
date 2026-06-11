# 本地生活智能点评与 AI 商家运营平台

本项目基于黑马点评课程项目升级，保留登录、店铺查询、优惠券秒杀、探店博客、点赞、关注、Feed 流、Redis 缓存和高并发链路，并新增 AI 商家运营助手、店铺智能问答 RAG、轻量级推荐 Agent。

## 技术栈

- Java 11
- Spring Boot 2.3.12.RELEASE
- Spring MVC
- MyBatis-Plus
- MySQL
- Redis / Lua / RedisTemplate
- Redisson
- Maven
- Vue + Axios + Element UI 静态前端
- Mock LLM Client

## 核心功能

- 手机验证码登录和 token 续期。
- 店铺分类、店铺列表、店铺详情、附近商户查询。
- 优惠券展示和秒杀下单。
- 探店博客发布、热门博客、博客详情。
- 点赞排行榜、关注、共同关注、Feed 流。
- Redis Bitmap 签到、HyperLogLog UV。
- AI 商家运营助手：文案、评论总结、运营建议。
- 店铺智能问答 RAG：基于店铺、券、博客证据回答。
- 轻量推荐 Agent：显式工具调用组合推荐店铺。

## Redis 高并发设计

- 使用 Hash 保存登录态。
- 使用 String JSON 缓存店铺和分类。
- 使用空值缓存解决缓存穿透。
- 使用逻辑过期和互斥锁解决热点缓存击穿。
- 使用 Lua 原子完成秒杀库存校验、一人一单和预扣库存。
- 使用 Redis List + processing 队列实现异步下单。
- 使用 ZSet 实现点赞排行和 Feed 流。
- 使用 Set 实现共同关注。
- 使用 GEO 实现附近商户。

## AI 功能设计

AI 模块统一位于 `/ai/**`，第一版使用 `MockLlmClient`，不需要真实 API Key。

新增接口：

- `POST /ai/llm/chat`
- `POST /ai/merchant/copywriting`
- `POST /ai/merchant/review-summary`
- `POST /ai/merchant/operation-advice`
- `POST /ai/shop/ask`
- `POST /ai/shop/reindex`
- `POST /ai/agent/recommend-shop`

设计约束：

- LLM 不直接访问数据库、Redis 或 SQL。
- Service 层显式读取业务数据并构造 Prompt。
- 返回 references，避免无证据胡编。
- LLM 失败时走降级文案，不让接口直接 500。

## 启动方式

后端：

```bash
cd hm-dianping
mvn spring-boot:run
```

运行前需要准备：

- MySQL 数据库 `hmdp`
- Redis 服务
- 本地 `application.yaml`，配置数据库和 Redis 地址

说明：仓库未提交包含本地密码的 `application.yaml`，请按自己的环境配置。

前端：

```text
nginx-1.18.0/html/hmdp
```

通过 nginx 访问静态页面，并将 `/api` 代理到后端。

## 测试方式

```bash
cd hm-dianping
mvn test
```

当前测试以不依赖 MySQL/Redis 的契约测试和单元测试为主。

## 项目亮点

- 用 Redis + Lua + 异步队列实现秒杀削峰。
- 用 Redisson 和 DB 条件扣减兜底超卖。
- 用接口契约测试保护旧前端接口。
- 在传统 Java 后端项目中加入可运行的 AI 应用后端能力。
- RAG 和 Agent 都基于真实本地生活业务数据，不停留在概念层。
