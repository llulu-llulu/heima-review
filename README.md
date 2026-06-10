# 黑马点评项目备份

这是一个黑马点评练习项目备份仓库，包含 Spring Boot 后端、HMDP 前端静态页面、数据库脚本、JMeter 压测脚本和素材文件。

## 项目结构

```text
.
├── hm-dianping/              # Spring Boot 后端项目
├── nginx-1.18.0/html/hmdp/   # 前端静态页面
├── nginx-1.18.0/             # 本地 nginx 运行目录
├── jedis-demo/               # Redis/Jedis 相关练习目录
├── 素材/                     # 项目素材
├── hmdp.sql                  # 数据库初始化脚本
├── 秒杀抢购.jmx              # JMeter 压测脚本
└── 代码片段.java             # 代码片段记录
```

## 技术栈

- Java 11
- Spring Boot 2.3.12.RELEASE
- MyBatis-Plus 3.4.3
- MySQL
- Redis
- Redisson
- Maven
- Nginx

## 后端运行

进入后端目录：

```bash
cd hm-dianping
```

启动前请先准备：

- MySQL 数据库：`hmdp`
- Redis 服务：默认连接 `127.0.0.1:6379`
- 数据库脚本：可使用根目录 `hmdp.sql` 或 `hm-dianping/src/main/resources/db/hmdp.sql`

当前后端配置文件位于：

```text
hm-dianping/src/main/resources/application.yaml
```

默认配置项：

- 服务端口：`8081`
- MySQL 地址：`127.0.0.1:3306/hmdp`
- MySQL 用户名和密码：请按本地环境修改
- Redis 密码：请按本地环境修改

使用 Maven 启动：

```bash
mvn spring-boot:run
```

## 前端运行

前端页面位于：

```text
nginx-1.18.0/html/hmdp
```

可通过 nginx 提供静态页面访问。nginx 配置文件位于：

```text
nginx-1.18.0/conf/nginx.conf
```

## 说明

本仓库用于项目备份和学习记录。备份时已排除 IDE 配置、构建产物、日志文件和嵌套 Git 元数据。
