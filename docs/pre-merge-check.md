# 合并前验收检查

检查分支：`feature/ai-upgrade-dianping`

对比基准：`origin/main`

检查时间：2026-06-11

## 1. 合并建议

结论：暂不建议直接合并到 `main`。

原因：

1. `origin/main` 与当前 feature 分支没有共同 merge base，属于历史不相连的分支。直接合并需要处理 unrelated histories。
2. 用 `git diff origin/main HEAD` 做树对比时，发现 feature 相对 `origin/main` 表现为大量删除，包括：
   - `nginx-1.18.0/**` 前端和 nginx 资源。
   - 根目录 `hmdp.sql`。
   - `hm-dianping/src/main/resources/application.yaml`。
   - `hm-dianping/src/main/resources/db/hmdp.sql`。
   - `jedis-demo/**`。
   - 素材和 JMeter 文件。
3. 这些文件当前在工作区仍以未跟踪形式存在，说明本轮提交没有把它们纳入 feature 分支。直接合并会导致主干上的前端静态资源和数据库脚本被删除。

建议先做一次“仓库结构整理提交”或从 `main` 正确创建新分支，再 cherry-pick 本轮有效提交。

## 2. 当前工作区状态

`git status --short --branch` 显示：

```text
## feature/ai-upgrade-dianping...origin/feature/ai-upgrade-dianping
?? hm-dianping/src/main/resources/application.yaml
?? hm-dianping/src/main/resources/db/
?? hm-dianping/src/test/java/com/hmdp/HmDianPingApplicationTests.java
?? hmdp.sql
?? jedis-demo/
?? nginx-1.18.0/
?? 代码片段.java
?? 秒杀抢购.jmx
?? 素材/
```

说明：

- 没有已暂存或已修改但未提交的跟踪文件。
- 存在大量未跟踪文件，这些文件多为主干已有资源或本地环境文件。
- 本次验收不会把未跟踪的配置、数据库脚本、前端资源、大文件纳入提交。

## 3. 差异范围

### 修改过的旧 Controller

相对 `origin/main`，旧 Controller 中只有以下两个文件有内容修改：

- `hm-dianping/src/main/java/com/hmdp/controller/BlogController.java`
- `hm-dianping/src/main/java/com/hmdp/controller/ShopController.java`

修改内容：

- `BlogController.queryHotBlog`
- `BlogController.queryBlog`
- `BlogController.queryLikes`
- `ShopController.queryShopById`

上述修改没有改 URL、HTTP 方法或请求参数，只是去掉了 `Result.ok(serviceResult)` 的二次包装，修复前端实际需要的返回结构。

### 修改过的 Result / DTO / VO

旧 DTO：

- `Result`
- `LoginFormDTO`
- `ScrollResult`
- `UserDTO`

这些旧 DTO 相对 `origin/main` 没有内容修改。

新增 AI DTO：

- `AgentRecommendRequest`
- `AgentRecommendResponse`
- `KnowledgeDocument`
- `LlmRequest`
- `LlmResponse`
- `MerchantCopywritingRequest`
- `MerchantCopywritingResponse`
- `MerchantOperationAdviceRequest`
- `MerchantOperationAdviceResponse`
- `MerchantReviewSummaryRequest`
- `MerchantReviewSummaryResponse`
- `ShopAskRequest`
- `ShopAskResponse`

### 修改过的拦截器、WebMvcConfig、Redis 工具类、秒杀相关 Service

修改文件：

- `hm-dianping/src/main/java/com/hmdp/config/RedissonConfig.java`
- `hm-dianping/src/main/java/com/hmdp/config/WebExceptionAdvice.java`
- `hm-dianping/src/main/java/com/hmdp/utils/CacheClient.java`
- `hm-dianping/src/main/java/com/hmdp/utils/RedisIdWorker.java`
- `hm-dianping/src/main/java/com/hmdp/utils/RefreshTokenIntercepter.java`
- `hm-dianping/src/main/java/com/hmdp/service/impl/ShopServiceImpl.java`

未修改：

- `hm-dianping/src/main/java/com/hmdp/config/MvcConfig.java`
- `hm-dianping/src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- `hm-dianping/src/main/resources/seckill.lua`

说明：

- `RefreshTokenIntercepter`：无 token 或无效 token 时放行，让登录拦截器处理受保护接口，避免匿名接口被误拦截。
- `CacheClient` / `ShopServiceImpl`：修复逻辑过期缓存重建锁判断反向问题。
- `RedisIdWorker`：修复时间戳使用 `getSecond()` 的错误。
- `RedissonConfig`：改为读取 `spring.redis.*` 配置，去掉硬编码 Redis 密码。
- `WebExceptionAdvice`：补充重复键、参数异常、通用异常处理，仍返回原 `Result` 结构。

## 4. 旧接口兼容性检查

结论：旧接口 URL、HTTP 方法、请求参数未被修改。

已通过 `ApiContractTest` 保护以下核心接口：

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

返回结构：

- `Result` 基础字段未改：`success`、`errorMsg`、`data`、`total`。
- 店铺详情和博客相关接口去掉二次包装后，更符合旧前端期望。

## 5. 新增 AI 接口检查

结论：新增 AI 接口全部位于 `/ai/**` 下。

接口列表：

- `POST /ai/llm/chat`
- `POST /ai/merchant/copywriting`
- `POST /ai/merchant/review-summary`
- `POST /ai/merchant/operation-advice`
- `POST /ai/shop/ask`
- `POST /ai/shop/reindex`
- `POST /ai/agent/recommend-shop`

## 6. 敏感信息和大文件检查

### API Key / token / 私钥

未发现已提交的真实 API Key、私钥或外部模型 token。

### 数据库密码

`application.yaml` 未提交在 feature 分支中，因此本轮没有提交本地数据库密码。

### Redis 密码

`RedissonConfig` 已从硬编码密码改为读取配置：

```text
spring.redis.password
```

### 本地路径

发现一个主干已有的本地路径常量：

```text
hm-dianping/src/main/java/com/hmdp/utils/SystemConstants.java
IMAGE_UPLOAD_DIR = "<absolute-workspace-path>\\nginx-1.18.0\\html\\hmdp\\imgs"
```

该问题不是本轮新增，但合并前建议改为配置项，例如 `hmdp.upload.dir`。

本轮新增文档中的绝对工作区路径已改为相对 `repo-root` 表述。

### 大文件

`git ls-tree -r -l HEAD` 未发现大于 1MB 的已提交文件。`nginx.exe`、图片素材等当前未被 feature 分支提交。

## 7. 工具痕迹检查

检查范围：`README.md` 和 `docs/**`

未发现以下工具指令痕迹：

- git branch 创建指令
- git commit 指令
- git push 指令
- git stage 指令
- git PR 创建指令

## 8. 测试结果

已执行：

```bash
cd hm-dianping
mvn test
```

结果：

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 9. Spring Boot 启动检查

环境端口检查：

- MySQL `127.0.0.1:3306`：可连通。
- Redis `127.0.0.1:6379`：可连通。

执行：

```bash
cd hm-dianping
mvn spring-boot:run
```

结果：

- 命令在 60 秒观察窗口内超时，未捕获到完整启动成功日志。
- 超时后发现两个 Java 进程出现在启动尝试时间段。由于无法安全确认它们是否属于本次启动，未强制结束，避免误杀用户其它 Java 进程。

结论：本轮只能确认编译和测试通过，不能确认服务已完整启动。需要人工在本地终端运行 `mvn spring-boot:run` 并检查启动日志和端口。

## 10. 前端风险点

必须阻止的合并风险：

- 由于 feature 分支相对 `origin/main` 表现为删除 `nginx-1.18.0/**`，直接合并会删除前端静态页面和 nginx 配置。

旧前端接口风险：

- `/user/logout` 后端仍返回“功能未完成”，这是旧问题，不是 AI 升级引入。
- 上传目录仍是本地绝对路径，跨机器运行风险较高。
- 前端依赖 `/api` 代理，需要 nginx 配置保留。

## 11. AI 模块风险点

- AI 第一版使用 Mock LLM，不代表真实模型效果。
- RAG 第一版用关键词检索，不支持向量召回和复杂语义匹配。
- 推荐 Agent 第一版没有真实融合 Redis GEO，只按数据库字段做候选和排序。
- AI 接口目前没有鉴权策略，后续上线前应接入登录态或后台权限。
- AI 服务读取业务数据后构造 Prompt，已避免让模型直接访问数据库或 Redis。

## 12. 必须人工验证的页面

合并前需要人工打开并验证：

- 登录页：`login.html`
- 首页分类与热门博客：`index.html`
- 店铺列表：`shop-list.html`
- 店铺详情和优惠券：`shop-detail.html`
- 秒杀下单按钮
- 博客发布：`blog-edit.html`
- 博客详情：`blog-detail.html`
- 我的主页和 Feed：`info.html`
- 他人主页、关注、共同关注：`other-info.html`

## 13. 必须人工验证的接口

旧接口：

- `POST /user/code`
- `POST /user/login`
- `GET /shop-type/list`
- `GET /shop/of/type`
- `GET /shop/{id}`
- `GET /voucher/list/{shopId}`
- `POST /voucher-order/seckill/{id}`
- `GET /blog/hot`
- `GET /blog/{id}`
- `PUT /blog/like/{id}`
- `GET /follow/or/not/{id}`
- `PUT /follow/{id}/{isFollow}`
- `GET /blog/of/follow`

AI 新接口：

- `POST /ai/llm/chat`
- `POST /ai/merchant/copywriting`
- `POST /ai/merchant/review-summary`
- `POST /ai/merchant/operation-advice`
- `POST /ai/shop/ask`
- `POST /ai/shop/reindex`
- `POST /ai/agent/recommend-shop`

## 14. 发现的问题和修复建议

### 必须处理后再合并

1. 解决 feature 和 `origin/main` 无共同历史的问题。
2. 避免合并时删除 `nginx-1.18.0/**`、数据库脚本和素材。
3. 确认是否要把 `application.yaml` 改为安全模板，例如 `application-example.yaml`，而不是删除。

### 建议处理

1. 将 `SystemConstants.IMAGE_UPLOAD_DIR` 改为配置项。
2. 实现 `/user/logout`，删除 Redis token 或至少与前端本地清理逻辑对齐。
3. 为 AI 接口增加登录鉴权或后台权限控制。
4. 为秒杀订单、关注关系增加唯一索引脚本。
5. 人工验证 Spring Boot 启动和前端页面。
