# 前端 AI 助手入口

## 本次新增内容

本次在黑马点评静态前端中增量新增 AI 助手页面，提供 AI 聊天测试、店铺智能问答、商家文案生成三个入口。实现方式为静态 HTML + CSS + 原生 JavaScript，不引入 Vue/React 工程化改造，不修改后端接口。

## 页面入口

- AI 页面地址：http://localhost:8080/ai.html
- 首页入口：`index.html` 右下角固定入口按钮「AI 助手」

## 新增文件列表

- `nginx-1.18.0/html/hmdp/ai.html`
- `nginx-1.18.0/html/hmdp/css/ai.css`
- `nginx-1.18.0/html/hmdp/js/ai.js`
- `docs/frontend-ai-entry.md`

## 接入的 AI 接口

- `POST /api/ai/llm/chat`
- `POST /api/ai/shop/ask`
- `POST /api/ai/merchant/copywriting`

## token 认证方式

AI 页面加载时会从以下位置读取 token：

- `sessionStorage.getItem("token")`
- `localStorage.getItem("token")`
- `sessionStorage.getItem("hmdp-token")`
- `localStorage.getItem("hmdp-token")`

请求 AI 接口时会自动携带请求头：

```text
authorization: token
```

页面未读取到 token 时，会显示「请先登录后使用 AI 助手」，并提供跳转 `login.html` 的按钮。页面不会展示或打印 token。

## 请求路径选择依据

现有前端 `js/common.js` 配置了：

```js
axios.defaults.baseURL = "/api";
```

nginx 配置中 `location /api` 会将请求转发到后端 `127.0.0.1:8081`，并通过 `rewrite /api(/.*) $1 break;` 去掉 `/api` 前缀。因此 AI 页面使用 `/api/ai/...` 路径，保持和旧前端接口一致。

## 使用方式

1. 启动后端 Spring Boot 服务，确认后端监听 `8081`。
2. 启动 nginx，确认前端监听 `8080`。
3. 打开 `http://localhost:8080/login.html` 登录。
4. 打开首页，点击右下角「AI 助手」，或直接访问 `http://localhost:8080/ai.html`。
5. 在 AI 页面分别测试 AI 聊天、店铺问答、商家文案生成。

## 人工测试清单

- `http://localhost:8080` 可以打开首页。
- 首页右下角可以看到「AI 助手」入口，点击后进入 `ai.html`。
- `http://localhost:8080/ai.html` 可以打开 AI 页面。
- 未登录时 AI 页面显示登录提示，并可跳转登录页。
- 登录后 AI 聊天测试可以返回内容，或显示后端返回的合理错误。
- 店铺智能问答可以返回结果，或显示后端返回的合理错误。
- 商家文案生成可以返回结果，或显示后端返回的合理错误。
- 首页、博客、店铺等原有页面仍可访问。

## 已知限制

- 当前 AI 后端可能使用 `MockLlmClient`，不是正式大模型。
- 商家文案生成依赖有效的 `shopId`。
- RAG 第一版可能是关键词检索或 Mock 返回，结果质量以后端当前实现为准。
- 当前页面只做基础调试和演示能力，未保存历史记录。

## 后续优化建议

- 接入真实大模型供应商。
- 增加更完整的页面样式和结果复制能力。
- 在店铺详情页接入 AI 按钮。
- 接入真实评论总结和运营建议。
- 增加请求历史记录和更细的 loading 状态。
