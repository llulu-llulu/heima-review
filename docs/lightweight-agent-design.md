# 轻量级推荐 Agent 设计

## 目标

新增接口：

```text
POST /ai/agent/recommend-shop
```

用户可以用自然语言表达需求，系统通过后端显式工具组合店铺、优惠券和内容证据，给出推荐。

## 工具边界

第一版实现以下后端工具方法：

- `searchNearbyShops`
- `getShopDetail`
- `getShopVoucherInfo`
- `getShopContentEvidence`
- `rankCandidateShops`

这些工具由 Java Service 调用现有业务 Service。LLM 不直接访问数据库、Redis 或 SQL。

## 请求示例

```json
{
  "query": "帮我找一家评分高、有优惠券、适合晚上吃的店",
  "typeId": 1,
  "maxCandidates": 5
}
```

## 响应字段

- `answer`：LLM 基于工具结果生成的推荐说明。
- `shops`：候选店铺。
- `reasons`：推荐理由。
- `usedTools`：实际调用过的工具。
- `references`：引用的店铺、券、博客证据。
- `needMoreInfo`：需求或候选不足时为 true。

## 工具日志

每次工具调用记录：

- `toolName`
- `arguments`
- `durationMs`
- `success`
- `errorMessage`

日志只记录参数摘要，不记录 token、手机号等敏感信息。

## 后续优化

1. 融合 Redis GEO 做真实附近排序。
2. 增加用户画像和历史点击偏好。
3. 将优惠券有效期、库存、营业时间纳入排序。
4. 用 RAG 检索出的博客/评论证据增强推荐解释。
