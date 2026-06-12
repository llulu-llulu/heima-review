# 项目升级工作日志

## 阶段 0：准备工作

时间：2026-06-11

### 当前项目结构

```text
repo-root
├── hm-dianping/              Spring Boot 后端项目，内部存在独立 .git
├── nginx-1.18.0/html/hmdp/   HMDP 前端静态页面，使用 Vue + Axios + Element UI
├── nginx-1.18.0/             本地 nginx 目录
├── jedis-demo/               Redis/Jedis 练习工程
├── docs/                     本次任务文档目录
├── hmdp.sql                  根目录数据库脚本
├── 秒杀抢购.jmx              JMeter 秒杀压测脚本
├── 代码片段.java             代码片段记录
└── 素材/                     图片和文本素材
```

### 项目形态判断

这是一个“后端 Spring Boot + 前端静态页面”的本地练习项目，不是现代前端工程化项目：

- 后端在 `hm-dianping`，使用 Maven 构建。
- 前端在 `nginx-1.18.0/html/hmdp`，页面直接通过静态 HTML、Vue、Axios 调用后端接口。
- 未发现 `package.json`、`src/api`、`npm build` 体系。

### 后端技术栈

- Java 11
- Spring Boot 2.3.12.RELEASE
- Spring MVC
- MyBatis-Plus 3.4.3
- MySQL Connector/J 5.1.47
- Redis / Spring Data Redis / Lettuce
- Redisson 3.22.0
- Hutool
- Lombok
- Lua 脚本：秒杀预扣库存、分布式锁释放
- Maven

### 前端技术栈

- Nginx 静态资源服务
- HTML
- Vue
- Axios
- Element UI
- 原生 JS/CSS

### 当前 Git 状态

根仓库当前分支已切换为：

```text
feature/ai-upgrade-dianping
```

准备阶段开始时，根仓库存在大量未跟踪文件：

```text
?? .gitignore
?? README.md
?? docs/
?? hm-dianping/
?? hmdp.sql
?? jedis-demo/
?? nginx-1.18.0/
?? 代码片段.java
?? 秒杀抢购.jmx
?? 素材/
```

注意事项：

- 根仓库没有配置远程地址。
- `hm-dianping/` 内部存在嵌套 `.git`，后续根仓库提交时不能盲目 `git add .`。
- `hm-dianping/target`、`.idea`、nginx 可执行文件、数据库密码配置等不应被本次任务误提交。
- 本次任务采用小步 stage 精确文件的方式推进。

### 后续执行计划

1. 阶段 1：扫描后端 Controller 和前端 Axios 调用，生成接口契约文档。
2. 阶段 2：补全工程审计文档，覆盖 Redis、高并发、事务、缓存一致性和面试亮点。
3. 阶段 3：补充接口契约保护测试或轻量测试计划，优先保证 `mvn test` 可运行。
4. 阶段 4：在不破坏旧接口的前提下做小范围工程化优化。
5. 阶段 5：新增不依赖真实 API Key 的 Mock LLM 基础架构。
6. 阶段 6：新增 `/ai/merchant/**` 商家运营助手接口。
7. 阶段 7：新增 `/ai/shop/**` 店铺问答 RAG 最小可运行版本。
8. 阶段 8：新增 `/ai/agent/recommend-shop` 轻量推荐 Agent。
9. 阶段 9：前端不具备工程化构建时，优先补充 AI 接入说明；如改前端，仅新增页面，不破坏旧页面。
10. 阶段 10-11：补齐 README、面试/简历文档、最终报告和测试验证记录。

### 风险说明

- 旧接口兼容性是最高优先级。AI 功能只新增 `/ai/**` 接口，不修改旧接口 URL、HTTP 方法、请求参数和 `Result` 结构。
- 当前项目依赖本地 MySQL 和 Redis，完整集成测试可能受环境影响；优先补 Mock 或不依赖外部服务的契约测试。
- 根仓库未完整跟踪项目，提交时必须避免敏感配置、大文件、构建产物和 IDE 配置。
- 若 `git push` 因无远程仓库失败，会记录到 `docs/git-sync-log.md`，但不中断后续开发。
