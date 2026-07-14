# 自动部署流程通用 SOP

> 本文档是 **前后端分离项目（前端 SPA + 后端 API）部署到 VPS、由 GitHub Actions 自动化** 的通用方法论。脱离子项目细节，可复用到同类项目。
> 项目特定的部署手册（密钥、域名、目录等）见各项目根目录的 `DEPLOY.md`。

---

## 适用场景

- 前端：Vue/Vite 或 React（构建产物是一堆静态文件）
- 后端：Spring Boot / Node / Go（构建产物是 jar / bundle / 二进制）
- 服务器：单台 VPS（Linux + systemd + nginx）
- 自动化：GitHub Actions（或 GitLab CI，原理相同）

如果你是「单体部署」或「容器化（K8s/ECS）」，本 SOP 的版本管理思想仍适用，但链路细节不同。

---

## 一、架构总览

```
   开发者 push 到 main
         │
         ▼
 ┌──────────────────────────────────────────┐
 │  CI（GitHub Actions）                       │
 │  ① checkout 代码                            │
 │  ② 构建后端产物（jar/binary）                │
 │  ③ 构建前端产物（dist），注入构建期变量       │
 │  ④ SCP 把产物传到服务器临时目录              │
 │  ⑤ SSH 远程执行部署脚本                      │
 └──────────────────────────────────────────┘
         │ SSH（专用密钥）
         ▼
 ┌──────────────────────────────────────────┐
 │  VPS                                       │
 │  部署脚本：产物存档（带时间戳）→ 改软链 →      │
 │           重启服务 → 清理旧版（留 N 个）      │
 │                                            │
 │  nginx → 前端静态 + 后端 API 反代            │
 └──────────────────────────────────────────┘
```

**核心思想：CI 负责构建，服务器只负责"摆好产物 + 切换 + 重启"。** 服务器上不跑构建，环境干净、资源稳定。

---

## 二、核心设计原理（为什么这么做）

### 1. 软链版本管理（原子切换 + 秒级回滚）

不放一个固定的 `app.jar`，而是：
- 每次发布存到 `releases/<产物名>-<时间戳>`
- 一个固定的软链 `current → releases/某版本` 指向当前生效版
- 切换 = 改软链指向（原子操作，瞬间完成）

好处：**回滚 = 把软链指回旧目录**，一行命令、秒级、不重新构建。

### 2. CI 构建而非服务器构建

- 服务器构建会污染运行环境（装 JDK/Node/Maven），抢生产资源
- CI 构建环境一致、可缓存、可并行
- 服务器只需 JRE/运行时 + 产物

### 3. 最小权限部署账号

CI 用**专用 deploy 账号**（不是你的日常 sudo 账号）：
- 只能写部署目录、只能重启指定 service（sudoers 精确限定）
- 密钥泄露时爆炸半径小
- 配 `NOPASSWD`（CI 非交互，没 tty 输密码）

### 4. 前端构建期变量走 Secret

SPA 的环境变量（`VITE_*` / `VUE_APP_*` / `REACT_APP_*`）是**构建时**打进产物的，运行时改不了。本地用 `.env.production` 文件，但 CI 拿不到（被 gitignore），**必须用 CI Secret 在构建步骤注入**。

---

## 三、前置准备 Checklist

### 服务器
- [ ] VPS 就绪，域名 DNS 解析到位，TLS（Let's Encrypt 或 Cloudflare 代理）
- [ ] 系统用户：运行服务的账号（`appuser`）+ 管理员（`admin`）+ CI 部署账号（`deploy`）
- [ ] 防火墙：只放行 22/80/443，中间件端口（DB/MQ/Cache）**只 localhost**

### 中间件
- [ ] 数据库（PostgreSQL/MySQL）建库建用户
- [ ] 缓存/队列（Redis/RabbitMQ）按需安装，确认默认只监听 localhost
- [ ] nginx 反代配置（前端 root + API upstream）

### 密钥与凭据
- [ ] 数据库密码、JWT 密钥、第三方 API key 等准备好（建议随机生成，不手写）
- [ ] 服务器侧：`.env.prod`（权限 600）+ systemd `EnvironmentFile=`
- [ ] CI 侧：SSH 私钥、前端构建变量 → 进 CI Secrets

### 仓库
- [ ] 部署工作流文件（`.github/workflows/deploy.yml`）
- [ ] 部署脚本（`deploy/deploy-backend.sh` / `deploy-frontend.sh` / `rollback.sh`）
- [ ] `.env.prod.example` 模板（真值不进库）

---

## 四、自动部署链路

### 工作流（`deploy.yml`）骨架

```yaml
on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # 构建后端
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: maven }
      - name: Build backend
        working-directory: backend          # ← 对齐你的实际目录名
        run: mvn -B package -DskipTests

      # 构建前端（关键：注入构建期变量）
      - uses: actions/setup-node@v4
        with: { node-version: '20', cache: npm, cache-dependency-path: frontend/package-lock.json }
      - name: Build frontend
        working-directory: frontend
        env:
          VITE_XXX_KEY: ${{ secrets.VITE_XXX_KEY }}   # ← 构建期变量从 Secret 注入
        run: |
          npm ci
          npm run build

      # 传产物到服务器
      - uses: appleboy/scp-action@v0.1.7
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          source: "backend/target/*.jar"
          target: "/tmp/deploy-incoming"
          strip_components: 2

      # 远程执行部署脚本
      - uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USER }}
          key: ${{ secrets.VPS_SSH_KEY }}
          script: |
            set -e
            bash /opt/app/deploy/deploy-backend.sh /tmp/deploy-incoming/app.jar
            bash /opt/app/deploy/deploy-frontend.sh /tmp/deploy-incoming/dist
            rm -rf /tmp/deploy-incoming
```

### 部署脚本骨架（服务器侧）

**`deploy-backend.sh`**（产物 → releases → 改软链 → 重启 → 清理）：

```bash
#!/bin/bash
set -euo pipefail
RELEASE_DIR="/opt/app/backend/releases"
LINK="/opt/app/backend/current.jar"
KEEP=5
NEW="$RELEASE_DIR/app-$(date +%Y%m%d-%H%M%S).jar"

cp "$1" "$NEW"                    # 存档
ln -sfn "$NEW" "$LINK"            # 切软链（原子）
sudo systemctl restart app.service
sleep 3
sudo systemctl is-active --quiet app.service || { echo "启动失败"; exit 1; }

# 清理旧版，留最近 N 个
cd "$RELEASE_DIR" && ls -t | tail -n +$((KEEP + 1)) | xargs -r rm -f
```

**`rollback.sh`**：`list` 列历史版本，`backend <版本>` / `frontend <版本>` 改软链指回。

---

## 五、首次接入 SOP（按顺序）

1. **服务器搭中间件**：DB/Cache/MQ，建库建用户
2. **建目录结构**：`releases/`、`shared/uploads/`、`deploy/`（脚本放这 + `chmod +x`）
3. **建 deploy 账号 + SSH 密钥**：私钥进 CI Secret，公钥进 `authorized_keys`
4. **目录权限**：deploy 加入 appuser 组，**父目录 + releases 都给组写**（见坑①）
5. **sudoers 免密**：`which systemctl` 确认路径，通配版限定单 service（见坑②）
6. **写 `.env.prod`**：从模板复制填真值，`chmod 600`
7. **systemd unit**：用 `EnvironmentFile=` 加载 `.env.prod`，别明文 `Environment=`
8. **nginx 配置**：前端 root 指向 `current` 软链，API 反代到后端端口
9. **CI Secrets**：`VPS_HOST`/`VPS_USER`/`VPS_SSH_KEY` + 前端构建变量
10. **首次 schema 对齐**：现有库用 `ddl-auto=update` 或 Flyway baseline 认领（见坑⑤⑥）
11. **push 触发 + 验证**：看 CI 全绿、服务 active、核心功能跑通

---

## 六、日常发布流程

```
开发 → 提交 feature 分支 → PR（跑构建检查 CI）→ 合并 main
   → 自动构建部署 → 看 Actions 绿 → 验证 → 完成
```

**改了 workflow 文件后**：必须 push 新 commit 触发新 run，**不能 Re-run**（Re-run 用的是当时 commit 的 workflow 旧版本）。

---

## 七、回滚

```bash
bash /opt/app/deploy/rollback.sh list                    # 看历史版本
bash /opt/app/deploy/rollback.sh backend app-20260713-1  # 切回指定后端版本
bash /opt/app/deploy/rollback.sh frontend frontend-20260713-1
```

回滚 = 改软链指向（+ 后端重启）。**秒级，不重新构建**。生产事故第一时间回滚，再排查。

---

## 八、常见坑与规避（通用版）

| # | 现象 | 根因 | 规避 |
|---|---|---|---|
| ① | `ln: Permission denied` 建软链失败 | 只给 `releases/` 权限，没给父目录 | 建软链要对**父目录**有写权限，父目录也要 `chgrp + chmod g+w` |
| ② | CI 报 `sudo: a terminal is required to read the password` | sudoers 路径写错或缺子命令 | `which systemctl` 确认真实路径，通配 `*` 覆盖所有子命令，限定单 service |
| ③ | 前端运行报 `Missing VITE_XXX_KEY` | 构建期变量 CI 没注入 | workflow build 步骤加 `env:`，值来自 CI Secret |
| ④ | CI 构建报 `Could not load .../xxx.vue`，本地正常 | import 路径大小写错（Win/Mac 不敏感，Linux 敏感） | 加 PR 阶段 CI 在 Linux 跑构建，提前抓；import 路径严格对齐目录大小写 |
| ⑤ | 运行时报 `column xxx does not exist` | `ddl-auto=update` 对有数据表加 NOT NULL 无 DEFAULT 列静默失败 | 手动 `ALTER ... ADD COLUMN ... DEFAULT`；或转 Flyway 正式管 |
| ⑥ | 切 `ddl-auto=validate` 后启动失败 | 新实体对应新表，生产库没有 | 首次临时 `ddl-auto=update` 建表，再改回 validate |
| ⑦ | 日志刷 `AmqpConnectException` / `RedisConnectionFailure` | 中间件没装但代码默认连 | 装中间件；或代码用 `@ConditionalOnProperty` 开关 + 配置关闭降级 |

---

## 九、运维加固

### 分层 CI（推荐）
- `ci.yml`：`on: [push, pull_request]`，所有分支跑构建检查（**不部署**）—— PR 阶段就抓大小写/编译错
- `deploy.yml`：只在 push main 时部署
- 两份分离，构建错误早发现，部署工作流保持单一职责

### 分支保护
- `main`：要求 PR + CI 通过 + 至少 1 review 才能合并
- 禁止直接 push main

### 备份
- DB：cron 定时 `pg_dump`/`mysqldump`，日备 + 周备，定期验证可恢复
- 用户上传文件：rsync 到异地或对象存储
- 备份要**演练恢复**，没验证过的备份等于没有

### 监控告警
- 后端暴露 `/actuator/health` + metrics
- systemd `OnFailure=` 触发告警 webhook
- 关键指标：服务存活、错误率、响应时间、DB连接数、磁盘

### 密钥管理
- 一律不进 Git（`.gitignore` 排除 `.env*` 真值文件）
- 服务器 `.env.prod` 权限 600，属主服务账号
- CI Secret 用最小集，定期轮换
- 密钥泄露立即轮换 + 审计访问日志

---

## 十、上线前自检清单

- [ ] CI 全绿（构建 + 部署脚本都过）
- [ ] 服务 `systemctl status` active
- [ ] 启动日志无 ERROR/Exception
- [ ] 健康检查端点 200
- [ ] 核心业务流程冒烟通过（登录、主功能、数据读写）
- [ ] 回滚脚本 `rollback.sh list` 能列出当前版本（确认可回滚）
- [ ] 监控/告警就位
- [ ] 旧版本在 `releases/` 留存（确认能回滚）

---

## 附：与容器化部署的取舍

本 SOP 是「**systemd + 软链**」模式，适合单 VPS、简单架构、想省心的场景。

如果遇到这些情况，考虑转 Docker/K8s：
- 多服务、多环境、需环境隔离
- 水平扩展（多实例）
- 跨机器部署
- 依赖复杂、环境漂移严重

容器化的版本管理思想一致（image tag = 这里的 release 目录，滚动更新 = 这里的软链切换），只是实现从 shell 变成 k8s/compose。
