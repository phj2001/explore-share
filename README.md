# 地点探索与打卡分享平台（discover-share_places）

一个基于地图的地点探索与内容分享全栈项目，覆盖**前台 C 端互动**与**后台运营治理**完整闭环，并已落地可观测性、异步通知、AI 探索助手与 CI/CD 自动部署等工程化能力。

> 仓库内部历史包名仍为 `smart-campus`（后端 Maven artifact、前端服务名），但产品定位已泛化为「地点探索与打卡分享平台」，可平滑迁移到校园、城市景点、商圈美食、本地探索路线等多种场景。

## 项目定位

核心能力围绕三条主线展开：

- **地图与地点探索**：地图中浏览地点、查看详情、收藏、评价、规划路线
- **内容互动**：地点打卡分享、图文相册、点赞回复、关注、成就、排行榜
- **运营治理**：公告、活动、推荐内容、举报审核、操作日志、文件资源、系统配置

## 技术栈

### 后端（`smart-campus-backend/`）

- **Spring Boot 4.0.3 · Java 21**
- Spring Security + JWT（鉴权 / 登录失败风控锁定）
- Spring Data JPA / Hibernate + **Flyway** 迁移管理
- **PostgreSQL + PostGIS**（地理空间）+ **pgvector**（向量检索）
- Hibernate Spatial + JTS（几何计算）
- **Redis**（用户信息缓存 / 登录风控 / AI 语义缓存）
- **RabbitMQ**（通知 Outbox 异步可靠投递 + 死信队列）
- Spring Boot Actuator + Micrometer + Prometheus（可观测性 + traceId 日志）
- Spring AOP（操作审计切面）
- Spring AI 2.0（OpenAI 兼容，接入 DashScope 通义千问）
- Spring Boot Mail（QQ SMTP）

### 前端（`smart-campus-frontend/`）

- **Vue 3.5 + Vite 7**
- Pinia 3（状态管理）+ Vue Router 5
- Element Plus 2.13 + Sass
- axios（HTTP 客户端）
- 高德地图 JS API（地图渲染 / 定位 / 路线）
- `unplugin-auto-import` + `unplugin-vue-components`（按需自动导入）

### 基础设施

- **Docker Compose** 一键编排（PostgreSQL/Redis/RabbitMQ/Prometheus/Grafana/Backend/Frontend）
- **GitHub Actions** 自动构建 + SCP + SSH 部署（`deploy.yml`）
- 生产环境 systemd + nginx，支持版本化发布与一键回滚

## 目录结构

```text
discover-share_places/
├── smart-campus-backend/      后端（Spring Boot 4 + Java 21）
│   ├── src/main/java/com/smartcampus/
│   │   ├── controller/        REST 接口（前台 + Admin 后台）
│   │   ├── entity/            JPA 实体
│   │   ├── service/           业务服务（接口 + impl）
│   │   ├── assistant/         AI 探索助手模块（RAG / 语义缓存 / 限流 / 护栏）
│   │   ├── messaging/         RabbitMQ 监听（通知 Outbox 投递）
│   │   ├── aspect/            AOP 切面（操作审计）
│   │   ├── security/          安全配置 / JWT 过滤器
│   │   ├── config/            各类配置（可观测性 / Amqp / Redis 等）
│   │   └── repository/        JPA 仓储
│   ├── src/main/resources/
│   │   ├── db/migration/      Flyway 迁移脚本（V1 基线 + 增量）
│   │   ├── db/legacy/         历史迁移脚本归档（非 Flyway 托管，仅留存）
│   │   └── application*.properties
│   ├── db/init/               扩展初始化（PostGIS / pgvector / 语义缓存表）
│   ├── Dockerfile / Dockerfile.db
│   └── pom.xml
├── smart-campus-frontend/     前端（Vue 3 + Vite）
│   ├── src/
│   │   ├── views/             页面（含 Admin/ 后台子目录）
│   │   ├── components/        组件（map / feed / route / assistant 等）
│   │   ├── stores/            Pinia 状态
│   │   ├── router/            路由（含后台权限守卫）
│   │   ├── api/ utils/ composables/
│   ├── Dockerfile + nginx.conf
│   └── package.json
├── deploy/                    生产部署脚本（deploy-backend / deploy-frontend / rollback）
├── ops/                       Prometheus 采集配置
├── docs/                      部署 SOP 等文档
├── .github/workflows/         CI/CD（deploy.yml）
├── docker-compose.yml         本地 / 演示一键编排
├── .env.example               Docker 编排环境变量模板
├── DEPLOY.md                  生产部署完整文档
└── README.md
```

## 主要功能模块

### 前台（C 端）

| 模块 | 说明 |
| --- | --- |
| 探索地图 | 高德地图浏览地点、定位、查看详情 |
| 地点详情（POI） | 基本信息、相册、评价、收藏 |
| 地点打卡分享 | 图文分享、多图相册、点赞、回复、标签 |
| 地点评价 / 收藏 / 打卡 | 多维度互动 |
| 用户路线 | 自定义路线 + 路点、收藏、点赞、提交审核 |
| 推荐路线 / 推荐分享 | 运营精选内容 |
| 平台公告 / 近期活动 | 资讯分发 |
| 动态流（Feed） | 聚合分享内容 |
| 排行榜 / 成就 | 用户活跃度与勋章 |
| 用户关注 / 用户主页 | 社交关系链 |
| 通知中心 | 站内通知（Outbox 异步投递） |
| 内容举报 | UGC 治理入口 |
| 地点申报 | 用户提交新地点，进入审核流 |
| AI 探索助手 | 基于地点语义检索的对话问答（RAG） |

### 后台（运营治理，`/admin`，超级管理员可见）

运营总览 · 地点管理（增改）· 地点分类 · 地点审核（申报）· 用户管理 · 分享管理 · 回复管理 · 评价管理 · 平台公告 · 活动管理 · 路线管理 · 路线审核 · 推荐内容 · 举报审核 · 系统日志（操作审计）· 文件资源 · 系统配置 · 通知投箱管理。

## 运行方式

项目支持三种运行模式，按需选择。

### 方式一：本地开发（前后端分离调试）

**后端**

```bash
cd smart-campus-backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

需准备本地 PostgreSQL（建议 PostGIS 镜像）+ Redis；本地配置见 `src/main/resources/application-dev.properties`，敏感值通过 `config/application-local.properties`（不进 Git）覆盖。

**前端**

```bash
cd smart-campus-frontend
npm install
npm run dev                     # 默认 http://localhost:5173
```

前端环境变量见 `smart-campus-frontend/.env.example`（含 `VITE_API_BASE_URL`、高德 JS Key 等）。

### 方式二：Docker Compose 一键编排（推荐用于联调 / 演示）

```bash
cp .env.example .env            # 填入真实值（数据库密码、JWT_SECRET、高德 Key 等）
docker compose up -d
```

启动后包含：

- 前端：http://localhost:5173
- 后端：http://localhost:8080（健康检查 `/actuator/health`）
- Grafana：http://localhost:3000
- Prometheus：http://localhost:9090
- RabbitMQ 控制台：http://localhost:15672

> 数据库镜像基于 `postgis/postgis` 额外编译加装 pgvector（详见 `smart-campus-backend/Dockerfile.db`）。首次启动需 `DDL_AUTO=update` 建全量表，成功后改回 `validate`，后续结构变更一律走 Flyway。

### 方式三：生产部署（systemd + nginx + CI/CD）

完整流程见 [DEPLOY.md](./DEPLOY.md) 与 [docs/deploy-sop.md](./docs/deploy-sop.md)。要点：

- 推送 `main` 分支触发 `.github/workflows/deploy.yml`：CI 构建 jar 与 dist → SCP 上传 → SSH 执行 `deploy/` 脚本
- 后端以 `place-explore-backend.service`（systemd）运行，版本化发布目录 + `app.jar` 软链接
- 前端由 nginx 托管，版本化 release 目录 + `current` 软链接
- `deploy/rollback.sh` 支持一键回滚到历史版本
- 生产环境变量模板见 `smart-campus-backend/.env.prod.example`（部署到服务器 `/opt/place-explore/backend/.env.prod`，权限 600）

## 环境变量速览

| 文件 | 用途 |
| --- | --- |
| `.env.example` | Docker Compose 编排总模板（数据库 / Redis / RabbitMQ / AI 等） |
| `smart-campus-backend/.env.prod.example` | 生产 systemd 部署模板 |
| `smart-campus-frontend/.env.example` | 前端构建变量（API 地址 / 高德 JS Key / 坐标系） |

关键开关：

- `ASSISTANT_ENABLED`：AI 探索助手总开关，默认 `false`，关闭时主功能完全不受影响
- `RABBITMQ_ENABLED`：通知异步投递开关，无 RabbitMQ 时可设 `false` 走同步降级
- `DDL_AUTO`：首次空库用 `update` 建表，之后改 `validate`；结构变更走 Flyway
- `FLYWAY_ENABLED`：数据库迁移开关

## 工程化亮点

- **可观测性**：Actuator 隔离到**独立 management 端口 9091**（主端口 8080 不暴露 `/actuator`），仅放行 `health/info/prometheus/metrics`，Prometheus 采集 + Grafana 可视化，日志携带 `traceId`
- **安全**：JWT 鉴权 + 登录失败次数风控锁定 + CORS 白名单 + 超级管理员初始化
- **内容安全**：UGC 入口接入**敏感词过滤**（DFA 多模式匹配，命中脱敏为 `***`），与事后举报形成「事前审核 + 事后举报」闭环
- **可靠性**：通知采用 Outbox 模式，经 RabbitMQ 异步投递，消费失败重试 3 次后进死信队列
- **数据治理**：Flyway 版本化迁移（`V1__baseline.sql`）+ 历史脚本归档至 `db/legacy/`，Hibernate `validate` 双保险，PostGIS 空间索引 + pgvector 向量检索
- **可维护性**：文件存储收口为 `StorageService` 抽象（接口 + `StorageCategory` 分类 + `LocalFileStorage` 实现），缩略图按分类尺寸下沉，未来可无感切换 OSS/MinIO
- **AI 工程化**：RAG 语义检索（POI 向量化）、语义缓存、按用户限流、输入长度护栏、会话记忆、用户偏好、检索评测；助手关闭时通过 `EnvironmentPostProcessor` 动态排除 Spring AI autoconfigure，api-key 可空（已去除占位串 hack）
- **CI/CD**：GitHub Actions 自动构建部署 + 版本化发布 + 一键回滚

## 后续可继续打磨的方向

- 前端分包与首屏性能优化
- 更细粒度的权限模型（区分管理员 / 超级管理员 / 普通用户）
- 路线体验与地图交互进一步打磨
- 搜索与推荐策略优化
- 内容审核策略升级（图片识别 / 接入第三方敏感词服务）
- 文件资源清理自动化
- 接口测试与端到端自动化测试覆盖
