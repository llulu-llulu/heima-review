# 前端 AI 接入说明

## 当前前端形态

项目中的前端不是 npm 工程，而是静态页面：

```text
nginx-1.18.0/html/hmdp
```

使用：

- HTML
- Vue
- Axios
- Element UI

统一请求配置在：

```text
nginx-1.18.0/html/hmdp/js/common.js
```

当前 Axios 基础地址：

```js
axios.defaults.baseURL = "/api";
```

因此新增 AI 页面时，请继续使用相同请求方式，访问路径写 `/ai/**`，最终会通过 `/api/ai/**` 代理到后端。

## 可新增页面

建议新增独立页面，不改旧页面核心逻辑：

- `ai-merchant.html`
- `ai-shop-qa.html`
- `ai-recommend.html`

旧页面保持不变：

- `login.html`
- `index.html`
- `shop-list.html`
- `shop-detail.html`
- `blog-edit.html`
- `blog-detail.html`
- `info.html`
- `other-info.html`

## 接口调用示例

### 商家文案

```js
axios.post("/ai/merchant/copywriting", {
  shopId: 1,
  activityType: "新品推广",
  targetUser: "周末聚餐用户",
  tone: "活泼",
  extraRequirement: "突出优惠券"
}).then(({ data }) => {
  console.log(data.title)
})
```

### 店铺问答

```js
axios.post("/ai/shop/ask", {
  question: "推荐一家适合晚上吃饭的店",
  typeId: 1,
  limit: 5
}).then(({ data }) => {
  console.log(data.answer)
})
```

### 推荐 Agent

```js
axios.post("/ai/agent/recommend-shop", {
  query: "帮我找一家评分高、有优惠券的店",
  typeId: 1,
  maxCandidates: 5
}).then(({ data }) => {
  console.log(data.shops)
})
```

## 兼容性要求

1. 不修改 `common.js` 的响应拦截器。
2. 不修改旧页面已有接口调用。
3. AI 页面只调用 `/ai/**`。
4. AI 页面失败时展示 `errorMsg`，不要影响旧页面。
5. 如果需要导航入口，建议新增一个独立链接，不改旧页面数据加载链路。

## 本阶段处理

本阶段只新增接入说明，不修改前端代码，因此不会影响旧前端页面。
