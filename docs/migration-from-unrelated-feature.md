# Migration From Unrelated Feature Branch

## 结论

建议使用 `feature/ai-upgrade-dianping-v2` 创建新的 PR，不建议继续合并旧的 `feature/ai-upgrade-dianping`。

本次迁移从 `origin/main` 新建干净分支，仅按白名单迁移 AI 模块、兼容性修复、测试和文档，未使用 merge、rebase、reset hard、clean 或 force push。

## 为什么不能直接合并旧 feature

旧分支 `origin/feature/ai-upgrade-dianping` 与 `origin/main` 没有共同 merge base。直接创建 PR 时，GitHub 会按两棵树做差异对比，旧 feature 会表现为删除大量 `main` 中已有资源。

风险包括：

- `nginx-1.18.0/**` 前端静态资源和 nginx 配置被删除。
- 数据库脚本 `hmdp.sql`、`hm-dianping/src/main/resources/db/hmdp.sql` 被删除。
- `素材/**`、压测脚本、示例资源等 main 资产被删除。
- `application.yaml` 在旧 feature 中缺失，直接合并会删除 main 的运行配置。

因此本次没有在旧分支上继续开发，也没有尝试合并旧 PR。

## 执行方式

原计划按顺序执行：

- `git fetch origin`
- `git checkout main`
- `git pull origin main`
- `git checkout -b feature/ai-upgrade-dianping-v2 origin/main`

实际执行时，`git checkout main` 被 Git 拦截，因为旧 feature 工作区存在大量未跟踪文件，而这些文件会被 `main` 的受管文件覆盖。为避免删除、清理或覆盖这些资源，本次改用独立 worktree 从 `origin/main` 创建干净迁移分支：

- 新分支：`feature/ai-upgrade-dianping-v2`
- 基线：`origin/main` `a7c7ef4 Add README`
- 迁移来源：`origin/feature/ai-upgrade-dianping` `87b64bf docs: add pre merge acceptance check`

## 从旧 feature 迁移的内容

### AI 后端模块

- `hm-dianping/src/main/java/com/hmdp/ai/**`
- `hm-dianping/src/main/resources/prompts/**`

新增接口均位于 `/ai/**`：

- `POST /ai/llm/chat`
- `POST /ai/shop/ask`
- `POST /ai/shop/reindex`
- `POST /ai/agent/recommend-shop`
- `POST /ai/merchant/copywriting`
- `POST /ai/merchant/review-summary`
- `POST /ai/merchant/operation-advice`

### 兼容性修复

- `RefreshTokenIntercepter`：无 token 或无效 token 时放行，由 `loginIntercepter` 继续负责受保护接口拦截，避免匿名接口被提前阻断。
- `ShopController.queryShopById`：返回 service 层 `Result`，避免 `Result.ok(Result)` 二次包裹。
- `BlogController.queryHotBlog/queryBlog/queryLikes`：返回 service 层 `Result`，避免二次包裹。
- `ShopServiceImpl`、`CacheClient`：修复逻辑过期缓存重建获取锁后的判断方向。
- `RedisIdWorker`：修复秒级时间戳计算，使用 epoch second，避免只取分钟内秒数导致 ID 时间段异常。
- `RedissonConfig`：改为从 `spring.redis.*` 读取配置，不再硬编码 Redis 密码。
- `WebExceptionAdvice`：补充重复键、参数校验和兜底异常处理。

### 测试代码

- `hm-dianping/src/test/java/com/hmdp/contract/**`

覆盖内容包括：

- AI 接口契约。
- 旧接口 URL、HTTP 方法、关键返回结构。
- 刷新 token 拦截器匿名放行行为。
- 本地 mock LLM 行为。
- Redis ID 时间戳回归。

### 文档

- `README.md`
- `docs/**`
- 新增本文档：`docs/migration-from-unrelated-feature.md`

## 明确未迁移的内容

以下内容没有从旧 feature 迁移：

- `hm-dianping/src/main/resources/application.yaml`
- 数据库密码或外部 API Key
- `target/**`
- `node_modules/**`
- `nginx-1.18.0/**`
- `nginx.exe`
- `hmdp.sql`
- `hm-dianping/src/main/resources/db/hmdp.sql`
- `素材/**`
- 本地环境产物和未确认来源素材

## 保留的 main 资源

迁移后已确认以下资源仍存在：

- `nginx-1.18.0/**`
- `nginx-1.18.0/html/hmdp/index.html`
- `hm-dianping/src/main/resources/db/hmdp.sql`
- `hmdp.sql`
- `素材/**`
- `hm-dianping/src/main/resources/application.yaml`

## 是否修改旧接口

未修改旧接口的 URL、HTTP 方法或请求参数。

旧接口返回结构存在小范围兼容性修复：

- `/shop/{id}`
- `/blog/hot`
- `/blog/{id}`
- `/blog/likes/{id}`

这些接口原先会把 service 层返回的 `Result` 再包一层 `Result.ok(...)`，前端实际读取 `data` 时会出现多一层 `success/errorMsg/data` 的结构。本次改为直接返回 service 层 `Result`，属于恢复既有接口契约，而不是新增业务功能。

## 敏感信息和工具痕迹检查

- README 和 docs 未发现 Codex Git 指令类工具痕迹。
- 未迁移真实 API Key、数据库密码、私钥或外部 token。
- AI 模块第一版使用 `MockLlmClient`，不依赖外部模型密钥。
- 未迁移大文件、nginx 可执行文件、`target/**`、`node_modules/**`。

## 测试结果

已执行：

- `cd hm-dianping`
- `mvn test`

结果：

- BUILD SUCCESS
- Tests run: 12
- Failures: 0
- Errors: 0
- Skipped: 0

## 启动检查

已检查：

- MySQL：`127.0.0.1:3306` 可连接。
- Redis：`127.0.0.1:6379` 可连接。
- `mvn spring-boot:run`：尝试在 60 秒窗口内启动，未能确认 `Started HmDianPingApplication`。

说明：

- 当前环境在启动检查前已有两个 Java 进程存在。
- PowerShell `Start-Process` wrapper 因重定向限制和 `Path/PATH` 环境键冲突未能启动应用。
- 通过 `cmd /c mvn "-Dspring-boot.run.fork=false" spring-boot:run` 进行 60 秒检查后超时，无法确认启动成功。
- 本轮检查产生的 Java 进程已停止，未继续长时间挂起。

## 建议

建议使用 `feature/ai-upgrade-dianping-v2` 创建新的 PR，并关闭或放弃旧的 `feature/ai-upgrade-dianping` PR。

合并前必须人工验证：

- 登录页：验证码、登录、登录态续期。
- 首页和店铺列表：`/shop/of/type`、`/shop-type/list`。
- 店铺详情：`/shop/{id}`、优惠券列表。
- 博客详情和点赞：`/blog/{id}`、`/blog/likes/{id}`、`/blog/like/{id}`。
- 关注 Feed：`/blog/of/follow`。
- 秒杀下单：`/voucher-order/seckill/{id}`。
- AI 新接口：全部 `/ai/**`。
