# Place Explore 部署文档

> 本文档是 **Place Explore（map.jieai.shop）项目专用的部署手册**，记录服务器拓扑、自动部署链路、首次接入步骤、日常发布流程，以及 2026-07-13 实战接入过程中踩过的所有坑。
> 通用方法论（适用于其他同类项目）见 [`docs/deploy-sop.md`](docs/deploy-sop.md)。

---

## 一、服务器与中间件

| 项 | 值 |
|---|---|
| VPS | RackNerd |
| 域名 | `map.jieai.shop`（Cloudflare 解析） |
| 系统用户 | `placeapp`（运行后端、拥有部署目录）、`hongjie`（管理员 sudo）、`deploy`（CI 部署专用） |
| 操作系统 | Ubuntu（`cat /etc/os-release` 确认版本） |

### 中间件清单

| 中间件 | 用途 | 端口 | 安装确认 |
|---|---|---|---|
| PostgreSQL | 主数据库（库名 `place_explore`，用户 `place_app`） | 5432 | `sudo systemctl status postgresql` |
| RabbitMQ | 通知异步队列（outbox） | 5672（AMQP）、15672（管理界面） | `sudo systemctl status rabbitmq-server` |
| Redis | 缓存 / 登录风控计数 / 语义缓存 | 6379 | `sudo systemctl status redis-server` |
| nginx | 反向代理 | 80/443 | `sudo systemctl status nginx` |

> RabbitMQ / Redis 未装时，对应功能（通知异步、缓存）会降级或刷错，但**核心功能（地图/POI/Feed/登录）不受影响**。生产建议装齐。

### 中间件安装（首次）

```bash
# PostgreSQL（通常已装）
sudo apt-get install -y postgresql

# RabbitMQ（apt 自带 Erlang 依赖）
sudo apt-get install -y rabbitmq-server
sudo systemctl enable --now rabbitmq-server

# Redis
sudo apt-get install -y redis-server
sudo systemctl enable --now redis-server
```

> **RabbitMQ 内存调优**（小内存 VPS 必做，避免和 Java/PG 抢内存）：
> ```bash
> echo "vm_memory_high_watermark.relative = 0.2" | sudo tee -a /etc/rabbitmq/rabbitmq.conf
> sudo systemctl restart rabbitmq-server
> ```

---

## 二、目录结构

### 后端
```
/opt/place-explore/backend/
├── app.jar              → 软链，指向 releases/ 下当前生效版本
├── .env.prod            → 密钥环境变量，权限 600，不进 Git
├── config/
│   └── application-prod.properties   # Spring 配置，不进 Git
├── logs/
├── uploads/
└── releases/            # 历史 jar，保留最近 5 个
/opt/place-explore/shared/uploads/     # 用户上传文件，持久化，发布不影响
/opt/place-explore/deploy/             # 部署脚本（deploy-*.sh / rollback.sh）
/opt/place-explore/backups/{db,logs,uploads}   # 定时备份产物
```

### 前端
```
/var/www/place-explore/
├── current  → releases/frontend-YYYYMMDD-HHMMSS   # nginx root 指向这里
└── releases/            # 历史版本，保留最近 5 个
```

---

## 三、服务管理

- **后端 systemd**：`place-explore-backend.service`（`/etc/systemd/system/place-explore-backend.service`）
- **nginx**：前端 `map.jieai.shop` → root 指向 `current` 软链；后端 API `/api/` `/uploads/` → `127.0.0.1:8080`

### 常用命令速查

```bash
# 后端状态 / 日志
sudo systemctl status place-explore-backend.service
sudo journalctl -u place-explore-backend.service -n 100 --no-pager
sudo journalctl -u place-explore-backend.service -f          # 实时跟踪

# 重启
sudo systemctl restart place-explore-backend.service

# 当前生效版本
readlink -f /opt/place-explore/backend/app.jar
readlink -f /var/www/place-explore/current

# 中间件
sudo systemctl status postgresql rabbitmq-server redis-server nginx
```

---

## 四、自动部署链路

### 架构总览

```
  开发者 push main
        │
        ▼
┌─────────────────────────────────────────┐
│  GitHub Actions (.github/workflows/…)   │
│  ① checkout 代码                          │
│  ② Maven 构建后端 jar                     │
│  ③ npm run build 构建前端 dist            │
│    （注入 VITE_AMAP_JS_KEY 等 Secret）    │
│  ④ SCP 把 jar + dist 传到 /tmp/deploy-incoming │
│  ⑤ SSH 执行服务器端脚本                   │
└─────────────────────────────────────────┘
        │ SSH (VPS_SSH_KEY)
        ▼
┌─────────────────────────────────────────┐
│  服务器                                   │
│  deploy-backend.sh: jar→releases→改软链→重启 │
│  deploy-frontend.sh: dist→releases→改软链    │
└─────────────────────────────────────────┘
```

### `deploy.yml` 关键段

- 触发：`push` 到 `main`
- 后端：`working-directory: smart-campus-backend`，`mvn -B package -DskipTests`
- 前端：`working-directory: smart-campus-frontend`，`npm ci && npm run build`，**`env:` 注入 `VITE_AMAP_JS_KEY` / `VITE_AMAP_SECURITY_JS_CODE`**（见坑③）
- 传输：`appleboy/scp-action` 把 jar 和 dist 传到 `/tmp/deploy-incoming`
- 部署：`appleboy/ssh-action` 远程执行 `deploy-backend.sh` + `deploy-frontend.sh`

### 三个脚本职责

| 脚本 | 职责 |
|---|---|
| `deploy-backend.sh <jar>` | jar 存档（带时间戳）→ 切 `app.jar` 软链 → `systemctl restart` → 清理旧版（留 5 个） |
| `deploy-frontend.sh <dist>` | dist 存档 → 切 `current` 软链 → 清理旧版 |
| `rollback.sh list\|backend\|frontend` | 列出/切回历史版本 |

---

## 五、部署账号与权限

> ⚠️ 本节是 2026-07-13 实战踩坑的重点（坑①②）。**不要用日常 `hongjie` 账号做 CI 部署**，权限太大、密钥泄露风险高。

### 1. 创建 deploy 账号 + SSH 密钥

```bash
sudo useradd -m -s /bin/bash deploy

# 本地生成密钥对，私钥进 GitHub Secrets，公钥进服务器
ssh-keygen -t ed25519 -f deploy_key -C "github-actions-deploy"
sudo mkdir -p /home/deploy/.ssh
echo "公钥内容" | sudo tee -a /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys
```

### 2. 目录写权限（坑①：父目录也要组写）

deploy 要在 `/opt/place-explore/backend/` 下建 `app.jar` 软链、在 `/var/www/place-explore/` 下建 `current` 软链。**只给 `releases/` 子目录权限不够**——建软链需要对**父目录**有写权限。

```bash
sudo usermod -aG placeapp deploy                       # deploy 加入 placeapp 组
sudo chgrp placeapp /opt/place-explore/backend /var/www/place-explore
sudo chmod g+w /opt/place-explore/backend /var/www/place-explore   # 父目录组可写
sudo chmod -R g+w /opt/place-explore/backend/releases /var/www/place-explore/releases
```

验证：

```bash
sudo -u deploy bash -c 'touch /opt/place-explore/backend/.permtest && rm /opt/place-explore/backend/.permtest && echo 后端OK'
sudo -u deploy bash -c 'touch /var/www/place-explore/.permtest && rm /var/www/place-explore/.permtest && echo 前端OK'
```

### 3. sudoers 免密（坑②：路径要真实 + 覆盖 is-active）

deploy 跑 `deploy-backend.sh` 里有 `sudo systemctl restart/is-active`。非交互 SSH 没 tty 输密码，必须配 NOPASSWD。

```bash
# 先确认 systemctl 真实路径（sudoers 要求全路径，写错不匹配）
which systemctl        # 多数 Ubuntu 是 /usr/bin/systemctl
```

写入 sudoers（推荐**通配版**，覆盖 restart/is-active/status，只限这一个 service）：

```bash
echo 'deploy ALL=(ALL) NOPASSWD: /usr/bin/systemctl * place-explore-backend.service' \
  | sudo tee /etc/sudoers.d/deploy-place-explore
sudo chmod 440 /etc/sudoers.d/deploy-place-explore
sudo visudo -cf /etc/sudoers.d/deploy-place-explore   # 必须 parsed OK
```

验证免密（只查状态，不重启）：

```bash
sudo -u deploy sudo -n /usr/bin/systemctl is-active place-explore-backend.service
# 不报 "a password is required" = 生效
```

> **坑②根因**：早期版本写的是 `/bin/systemctl`，但服务器实际是 `/usr/bin/systemctl`，sudo 匹配不上 → 所有命令都退回要密码 → CI 报 `a terminal is required to read the password`。

---

## 六、环境变量与密钥

### 后端 `.env.prod`（服务器 `/opt/place-explore/backend/.env.prod`，权限 600，不进 Git）

systemd unit 用 `EnvironmentFile=` 加载。完整变量见 [`smart-campus-backend/.env.prod.example`](smart-campus-backend/.env.prod.example)，关键项：

```properties
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_PASSWORD=<真实DB密码>
JWT_SECRET=<JWT密钥>
SUPER_ADMIN_USERNAME=<超管账号>
SUPER_ADMIN_PASSWORD=<超管密码>
AMAP_WEB_KEY=<高德Web Service key>     # 后端地理编码用，区别于前端 JS key
DDL_AUTO=validate                       # 生产常态！首次新实体建表时临时改 update
RABBITMQ_ENABLED=true                   # 装了 RabbitMQ 才 true，否则 false 走同步降级
# Redis / Mail / DashScope 等按需
```

### 前端构建变量（GitHub Secrets，构建时注入）

| Secret | 说明 |
|---|---|
| `VPS_HOST` | 服务器 IP 或域名 |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | 部署私钥全文 |
| `VITE_AMAP_JS_KEY` | 高德 **JS API** key（前端地图，区别于后端 Web key） |
| `VITE_AMAP_SECURITY_JS_CODE` | 高德安全密钥 |

> **坑③④**：Vite 的 `VITE_` 变量在 `npm run build` 时打进产物。本地读 `.env.production` 文件，但 CI 拿不到（被 gitignore），必须用 Secret 注入（见 `deploy.yml` 的 `env:`）。值从本地 `smart-campus-frontend/.env.production` 复制。

### 高德控制台

JS API key 的**域名白名单**要加 `map.jieai.shop`（本地开发一般只配了 `localhost`），否则浏览器报 `INVALID_USER_DOMAIN`。

---

## 七、数据库 schema 管理

本项目采用 **Flyway 主导 + Hibernate 校验** 的混合策略：

- `spring.flyway.baseline-on-migrate=true` + `baseline-version=0`：既有库自动认领为 baseline
- `V1__baseline.sql` 是空标记（`SELECT 1`），不重建已有表
- `spring.jpa.hibernate.ddl-auto=${DDL_AUTO:validate}`：**生产默认 validate**，只校验不自动改表

### 首次发布含新实体时（坑⑤⑥）

新实体（如本次的 AI 助手会话记忆/语义缓存等）对应新表，生产库没有。validate 会启动失败。**临时改 `DDL_AUTO=update`** 让 Hibernate 建表，之后再改回 validate：

```bash
echo "DDL_AUTO=update" | sudo tee -a /opt/place-explore/backend/.env.prod
# push 部署 → 建表 → 验证服务起来后：
sudo sed -i 's/^DDL_AUTO=update$/DDL_AUTO=validate/' /opt/place-explore/backend/.env.prod
sudo systemctl restart place-explore-backend.service
```

### 已知 schema 漂移修复（坑⑤）

`ddl-auto=update` 对「**有数据的表 + NOT NULL 无 DEFAULT 的新列**」会静默失败（`column contains null values`），服务照样起，运行时才报错。本次 `users.can_reset_password` 就是：

```bash
sudo -u postgres psql -d place_explore -c \
  "ALTER TABLE users ADD COLUMN IF NOT EXISTS can_reset_password BOOLEAN NOT NULL DEFAULT false;"
```

> 排查方法：`sudo journalctl -u place-explore-backend.service | grep -iE "unsuccessful|contains null|add column"`，每条 `Unsuccessful: ALTER TABLE ... ADD COLUMN ...` 就是一个漂移列。

---

## 八、日常发布流程

1. 本地开发 → 提交到 feature 分支
2. （建议）PR 阶段先跑构建检查（见运维加固的 `ci.yml`），确认 Linux 下能编译通过
3. 合并到 `main`（或直接 push main）
4. GitHub Actions 自动构建部署 → Actions 页看是否全绿
5. 浏览器/服务器验证（服务 active、核心功能正常）
6. 出问题用回滚（见第九节）

> **重要**：改了 `deploy.yml` 后**必须 push 新 commit 触发新 run**，不能 Re-run（Re-run 用的是旧 workflow）。

---

## 九、回滚

```bash
# 查看可回滚的历史版本
bash /opt/place-explore/deploy/rollback.sh list

# 回滚后端到指定 jar
bash /opt/place-explore/deploy/rollback.sh backend smart-campus-backend-0.0.1-SNAPSHOT-20260405-091400.jar

# 回滚前端到指定 release
bash /opt/place-explore/deploy/rollback.sh frontend frontend-20260405-01
```

---

## 十、首次接入 Checklist

### 服务器侧
- [ ] 装/确认 PostgreSQL、RabbitMQ、Redis、nginx
- [ ] 建库建用户：`place_explore` / `place_app`
- [ ] 创建 `deploy` 账号 + SSH 公钥 + sudoers（**通配版 + 正确 systemctl 路径**，见第五节）
- [ ] 目录权限：父目录 + releases 都给 placeapp 组写（见第五节）
- [ ] 三个脚本放 `/opt/place-explore/deploy/` 并 `chmod +x`
- [ ] 创建 `.env.prod`（从 `.env.prod.example` 复制，填真值，`chmod 600`）
- [ ] systemd unit 用 `EnvironmentFile=/opt/place-explore/backend/.env.prod`（不要明文 `Environment=`）
- [ ] nginx 配好前端 root + `/api` 反代
- [ ] ufw 防火墙：只放行 22/80/443，**不放行** 5672/15672/5432/6379（中间件只 localhost 访问）

### GitHub 侧
- [ ] Secrets：`VPS_HOST` / `VPS_USER` / `VPS_SSH_KEY`
- [ ] Secrets：`VITE_AMAP_JS_KEY` / `VITE_AMAP_SECURITY_JS_CODE`
- [ ] `deploy.yml` 里 JDK 版本对齐服务器（`java -version` 确认）
- [ ] `deploy.yml` 里 `working-directory` 对齐实际目录名（`smart-campus-backend/-frontend`）

### 高德控制台
- [ ] JS API key 白名单加 `map.jieai.shop`

### 验证
- [ ] push main 跑通一次自动部署
- [ ] 浏览器：地图加载、POI 符号、登录、核心流程

---

## 十一、常见问题与坑（实战实录）

| # | 现象 | 根因 | 解决 |
|---|---|---|---|
| ① | `ln: failed to create symbolic link '.../app.jar': Permission denied` | 只给了 `releases/` 组写，没给父目录 | 父目录 `chgrp placeapp + chmod g+w`（第五节） |
| ② | `sudo: a terminal is required to read the password` | sudoers 路径写错（`/bin` vs `/usr/bin`）或缺 `is-active` | `which systemctl` + 通配版 sudoers（第五节） |
| ③ | 前端报 `Missing VITE_AMAP_JS_KEY` | Vite 变量构建时注入，CI 没配 | `deploy.yml` build frontend 加 `env:` + GitHub Secret |
| ④ | CI 构建报 `Could not load .../views/admin/POIApplicationList.vue` | router 里 import 路径大小写错（Windows 不敏感，Linux 敏感） | 改成实际目录大小写 `views/Admin/`；加 PR 阶段 CI 提前抓 |
| ⑤ | 运行时报 `column xxx does not exist` | `ddl-auto=update` 对有数据表加 NOT NULL 无 DEFAULT 列失败 | `ALTER TABLE ... ADD COLUMN ... DEFAULT ...` 手动补 |
| ⑥ | 切 validate 后启动失败 `SchemaValidation` | 新实体对应新表生产库没有 | 首次临时 `DDL_AUTO=update` 建表，再改回 validate |
| ⑦ | 日志刷 `AmqpConnectException: Connection refused` | RabbitMQ 没装但开关默认 true | 装 RabbitMQ，或 `.env.prod` 设 `RABBITMQ_ENABLED=false` 降级 |

---

## 十二、运维加固（建议）

### 备份
- PostgreSQL：cron 定时 `pg_dump` 到 `/opt/place-explore/backups/db/`
- uploads：定时 rsync 到备份目录或异地
- 保留策略：日备 7 份 + 周备 4 份

### 监控
- Actuator + Prometheus 已在代码里埋点（`/actuator/prometheus`）
- 配 Prometheus 抓取 + Grafana 看板
- systemd `OnFailure=` 配告警 webhook

### 密钥轮换
- 2026-07-13 前曾明文出现在 systemd unit 的密钥（DB密码/JWT_SECRET/超管密码）**必须轮换**
- 部署 SSH 私钥只存 GitHub Secrets，不落地本地明文

### 分支保护
- `main` 分支：要求 PR + CI 通过才能合并
- 禁止直接 push main（走 PR 流程）

### PR 阶段 CI（防大小写/编译类坑）
- 新建 `.github/workflows/ci.yml`，`on: [push, pull_request]`，只跑前后端构建不部署
- 这样构建错误在 PR 阶段就暴露，不用等合并 main 部署时才炸

---

## 附录：安全提醒

- `.env.prod` / `application-prod.properties` / 部署私钥 **一律不进 Git**，仓库只留 `.env.prod.example` 模板
- 中间件端口（5432/5672/15672/6379）**不对公网开放**，靠 ufw 默认 deny + 仅 localhost 监听
- RabbitMQ 管理界面要走 SSH 隧道（`ssh -L 15672:localhost:15672 hongjie@map.jieai.shop`），不要 `ufw allow 15672`
- `guest` 用户只能 localhost 连 RabbitMQ（默认安全），不要改
