# 接口契约风险记录

## P0：Result 嵌套风险

以下 Controller 方法当前把 Service 返回的 `Result` 再次包装成 `Result.ok(...)`：

- `ShopController.queryShopById`
- `BlogController.queryHotBlog`
- `BlogController.queryBlog`
- `BlogController.queryLikes`

前端期望 `data` 直接是 `Shop`、`Blog` 或 `UserDTO[]`，但嵌套后会变成 `data.success/data.data`。这是旧接口兼容性风险，后续修复时应保持 URL、HTTP 方法和最终返回字段含义不变。

## P0：匿名接口可能被刷新 token 拦截器阻断

`RefreshTokenIntercepter` 当前在无 token 或 token 无效时直接 `return false`。由于它注册为 `/**` 且优先级高于登录拦截器，可能导致登录、验证码、店铺查询、热门博客等匿名接口不可用。

兼容修复方向：无 token 时放行，仅在 token 存在且有效时刷新 ThreadLocal 和 TTL，由 `loginIntercepter` 负责受保护接口的 401。

## P1：登出接口未实现

前端 `info.html` 调用 `POST /user/logout`，后端当前固定返回失败。后续可在不改变接口契约的前提下实现删除 Redis token 或提示前端本地清理 token。

## P1：严格参数校验可能破坏旧前端

旧前端部分请求依赖默认值或空字符串过滤，例如 `common.js` 的 `paramsSerializer` 会跳过 falsy 参数。对旧接口贸然新增严格校验可能导致前端旧调用失败。

建议：旧接口先用兼容校验和文档约束；新增 `/ai/**` 接口使用 `@Validated` 严格校验。

## P2：前端依赖 `/api` 代理

前端 `axios.defaults.baseURL = "/api"`，需要 nginx 或其他代理把 `/api` 转发到后端 `8081`。如果直接打开 HTML 文件或代理缺失，接口会失败。

建议：README 明确 nginx 代理配置和后端启动端口。
