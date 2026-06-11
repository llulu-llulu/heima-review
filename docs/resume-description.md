# 简历项目描述

## 项目名称

本地生活智能点评与 AI 商家运营平台

## 项目背景

基于黑马点评课程项目二次升级，面向本地生活场景，实现店铺点评、优惠券秒杀、社交 Feed 和 AI 商家运营能力，突出 Java 后端高并发、Redis 设计和大模型应用开发能力。

## 技术栈

Java 11、Spring Boot、MyBatis-Plus、MySQL、Redis、Lua、Redisson、Maven、Vue、Axios、Mock LLM、RAG、Agent。

## 个人职责

- 梳理并保护前后端接口契约，补充接口回归测试。
- 修复登录拦截器、ID 生成器、缓存重建锁和 Result 嵌套等工程风险。
- 设计 Redis 缓存、秒杀、点赞、关注、Feed 流等高并发链路。
- 新增 Mock LLM 基础设施、AI 商家运营助手、店铺 RAG 问答和推荐 Agent。
- 编写项目审计、Redis、秒杀、AI、简历和面试文档。

## 业务功能

- 用户登录、签到、UV。
- 店铺分类、详情、附近商户。
- 优惠券秒杀、一人一单、异步下单。
- 探店博客、点赞排行、关注、共同关注、Feed 流。
- AI 文案生成、评论总结、运营建议、店铺问答、店铺推荐。

## Redis 高并发亮点

- Lua 原子化秒杀库存预扣和一人一单。
- Redis List + processing 队列异步落库。
- Redisson 用户维度锁和 DB 条件扣减兜底超卖。
- ZSet 实现点赞排行榜和 Feed 滚动分页。
- GEO 实现附近商户查询。

## AI 大模型应用亮点

- 抽象 `LlmClient`，第一版使用 `MockLlmClient`，无 API Key 也能运行。
- Prompt 模板外置，支持后续接入真实模型。
- RAG 基于店铺、券、博客生成知识文档，返回 references 防胡编。
- 推荐 Agent 使用后端显式工具，不让模型直接访问数据库。
- LLM 异常时统一降级，不影响旧业务。

## 可量化表述建议

- “设计 Redis Lua 秒杀预扣库存链路，将高并发下单请求削峰为异步落库。”
- “补充 12 个接口契约与工程回归测试，保护登录、店铺、博客、秒杀和 AI 新接口。”
- “新增 7 个 `/ai/**` 接口，覆盖商家运营、RAG 问答和推荐 Agent。”

## 面试讲解版本

这个项目可以按三层讲：

1. 传统业务：登录、店铺、券、博客、关注、Feed。
2. 高并发：Redis 缓存、Lua 秒杀、Redisson 锁、异步队列。
3. AI 应用：Mock LLM、RAG、Agent、降级和接口兼容。
