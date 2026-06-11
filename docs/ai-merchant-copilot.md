# AI 商家运营助手

## 目标

在不修改旧店铺、博客、优惠券接口的前提下，新增 `/ai/merchant/**` 商家运营助手接口。第一版使用 `MockLlmClient`，不需要真实 API Key。

## 新增接口

### POST `/ai/merchant/copywriting`

请求：

```json
{
  "shopId": 1,
  "activityType": "新品推广",
  "targetUser": "周末聚餐用户",
  "tone": "活泼",
  "extraRequirement": "突出优惠券"
}
```

响应 `data`：

```json
{
  "title": "店铺名｜新品推广",
  "content": "LLM 生成文案",
  "sellingPoints": ["店铺：...", "评分：..."],
  "riskTips": ["文案必须以真实店铺信息为准"],
  "references": ["shop:1:店铺名", "voucher:2:券标题", "blog:3:博客标题"]
}
```

### POST `/ai/merchant/review-summary`

请求：

```json
{
  "shopId": 1,
  "maxReviews": 5
}
```

说明：当前 `tb_blog_comments` 没有直接的 `shop_id` 字段，因此第一版使用店铺关联的探店博客作为评论/内容证据来源。

### POST `/ai/merchant/operation-advice`

请求：

```json
{
  "shopId": 1,
  "businessGoal": "提升到店转化",
  "budgetLevel": "低"
}
```

## 数据来源

`MerchantCopilotService` 显式读取：

- `Shop`
- `Voucher`
- `Blog`

LLM 只接收后端整理后的上下文，不直接访问数据库、Redis 或 SQL。

## 降级策略

所有 LLM 调用通过 `LlmService`。如果 LLM 异常，返回兜底文案并保留结构化字段，接口不直接 500。

## 兼容性

- 不修改旧 Controller。
- 不修改旧 URL、HTTP 方法、请求参数和 `Result` 基础结构。
- 新接口全部位于 `/ai/merchant/**`。
