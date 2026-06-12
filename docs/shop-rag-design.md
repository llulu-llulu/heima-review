# 店铺智能问答 RAG 设计

## 目标

新增最小可运行版本的店铺智能问答：

```text
POST /ai/shop/ask
POST /ai/shop/reindex
```

第一版不引入向量数据库，使用现有 MySQL 数据生成知识文档，并基于关键词和简单打分检索。

## 知识文档

`KnowledgeDocument` 字段：

- `docId`
- `docType`
- `sourceId`
- `title`
- `content`
- `tags`
- `score`

来源：

- `Shop`：店铺名称、区域、地址、营业时间、评分、人均。
- `Voucher`：优惠券标题、副标题、规则、关联店铺。
- `Blog`：探店标题、内容、关联店铺。

## 问答流程

1. `AiShopController` 接收问题。
2. `ShopRagService` 校验问题非空。
3. `KnowledgeIndexService` 从现有数据生成知识文档并检索。
4. 检索不到证据时返回：

```text
当前数据不足，无法基于已有店铺信息回答
```

并设置 `needMoreInfo=true`。

5. 检索到证据后构造上下文。
6. 调用 `LlmService`，当前由 `MockLlmClient` 返回本地模拟答案。
7. 返回答案、推荐店铺、references、confidence。

## 防胡编策略

- LLM 不直接访问数据库。
- Service 明确整理证据并传入 Prompt。
- 响应必须返回 `references`。
- 无证据时拒答，不编造店铺。

## 后续扩展

1. 将 `KnowledgeDocument` 持久化到表。
2. 增加中文分词和同义词。
3. 引入 embedding 和向量数据库。
4. 增加用户定位和 Redis GEO 附近店铺融合排序。
