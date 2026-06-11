# 接口契约回归测试计划

## 目标

后续工程化优化和 AI 改造不能破坏既有 HMDP 前端正在调用的接口。测试优先保护：

- URL
- HTTP 方法
- `Result` 基础结构
- 核心业务接口是否仍存在

## 当前实现

已新增 `hm-dianping/src/test/java/com/hmdp/contract/ApiContractTest.java`。

该测试不启动 Spring Boot 上下文，不连接 MySQL 和 Redis，而是通过反射检查 Controller 注解和 `Result` 字段。这样可以在缺少外部服务的环境中快速失败，适合作为接口契约的第一道保护。

## 已保护接口

- `POST /user/code`
- `POST /user/login`
- `POST /user/logout`
- `GET /user/me`
- `GET /user/info/{id}`
- `GET /user/{id}`
- `GET /shop-type/list`
- `GET /shop/{id}`
- `POST /shop`
- `PUT /shop`
- `GET /shop/of/type`
- `GET /shop/of/name`
- `GET /voucher/list/{shopId}`
- `POST /voucher-order/seckill/{id}`
- `POST /blog`
- `PUT /blog/like/{id}`
- `GET /blog/of/me`
- `GET /blog/hot`
- `GET /blog/{id}`
- `GET /blog/likes/{id}`
- `GET /blog/of/follow`
- `GET /blog/of/user`
- `PUT /follow/{id}/{isFollow}`
- `GET /follow/or/not/{id}`
- `GET /follow/common/{id}`
- `POST /upload/blog`
- `GET /upload/blog/delete`

## 后续增强

1. 使用 `@WebMvcTest` 为核心 Controller 补 MockMvc 响应结构测试。
2. Mock Service 层返回，验证前端实际读取的 `data` 字段类型。
3. 为匿名接口补拦截器回归测试，防止 `/user/login`、`/shop/**` 被误拦截。
4. 为秒杀接口补 Lua 脚本返回码测试。
5. 为 `/ai/**` 新接口补参数校验和降级响应测试。

## 运行方式

```bash
cd hm-dianping
mvn test
```

如果 Maven 需要下载依赖，可能需要网络环境。
