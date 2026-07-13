# Place Explore 部署文档

## 服务器信息
- VPS: RackNerd，域名 map.jieai.shop（Cloudflare 解析）
- 系统用户: `placeapp`（运行后端进程和拥有部署目录），`hongjie`（管理员登录/sudo）

## 目录结构

### 后端
```
/opt/place-explore/backend/
├── app.jar          -> 软链，指向 releases/ 下当前生效版本
├── config/
│   └── application-prod.properties   # Spring 配置，不进 Git
├── .env.prod                          # 密钥环境变量，权限600，不进 Git
├── logs/
├── uploads/
├── releases/         # 历史jar版本，保留最近5个
└── ...
/opt/place-explore/shared/uploads/     # 用户上传文件，持久化，发布不影响
/opt/place-explore/backups/{db,logs,uploads}   # 定时备份产物
```

### 前端
```
/var/www/place-explore/
├── current -> releases/frontend-YYYYMMDD-HHMMSS   # nginx root 指向这里
└── releases/         # 历史版本，保留最近5个
```

### 服务管理
- systemd service: `place-explore-backend.service`
- 配置文件: `/etc/systemd/system/place-explore-backend.service`
- 数据库: PostgreSQL，库名 `place_explore`，用户 `place_app`

### 反向代理 (nginx)
- 前端: `map.jieai.shop` -> root 指向 current 软链
- 后端 API: `/api/` `/uploads/` -> `127.0.0.1:8080`

## 常用命令

```bash
# 查看后端服务状态 / 日志
sudo systemctl status place-explore-backend.service
sudo journalctl -u place-explore-backend.service -n 100 --no-pager

# 手动重启后端
sudo systemctl restart place-explore-backend.service

# 查看当前生效的前后端版本
readlink -f /opt/place-explore/backend/app.jar
readlink -f /var/www/place-explore/current
```

## 部署流程（自动化，通过 GitHub Actions）

1. 代码 push 到 `main` 分支
2. GitHub Actions 自动执行 `.github/workflows/deploy.yml`：
   - 构建后端 jar（Maven）
   - 构建前端（`npm run build`，Vue）
   - 通过 SSH 把产物传到服务器 `/tmp/deploy-incoming`
   - 依次执行 `deploy/deploy-backend.sh`、`deploy/deploy-frontend.sh`
3. 脚本自动完成：拷贝新版本 -> 切换软链 -> 重启服务 -> 清理旧版本（保留最近5个）

### 需要在 GitHub 仓库 Settings → Secrets and variables → Actions 配置

| Secret | 说明 |
|---|---|
| `VPS_HOST` | 服务器 IP 或域名 |
| `VPS_USER` | 建议专门建一个部署用户，而不是用 hongjie 或 placeapp 的交互账号 |
| `VPS_SSH_KEY` | 部署专用的 SSH 私钥（见下方"部署账号与权限"） |

## 部署账号与权限设置（建议，一次性配置）

不建议用你日常登录的 `hongjie` 账号做自动部署（权限太大，密钥一旦泄露风险高）。建议：

```bash
# 1. 创建专门的部署账号
sudo useradd -m -s /bin/bash deploy

# 2. 生成一对部署专用密钥（在本地或CI环境生成，私钥放进GitHub Secrets，公钥放服务器）
ssh-keygen -t ed25519 -f deploy_key -C "github-actions-deploy"
# 公钥内容加到:
sudo mkdir -p /home/deploy/.ssh
echo "公钥内容" | sudo tee -a /home/deploy/.ssh/authorized_keys
sudo chown -R deploy:deploy /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
sudo chmod 600 /home/deploy/.ssh/authorized_keys

# 3. 给 deploy 账号必要但最小化的 sudo 权限（只能重启这一个服务，不能干别的）
sudo visudo -f /etc/sudoers.d/deploy-place-explore
# 写入以下内容：
# deploy ALL=(ALL) NOPASSWD: /bin/systemctl restart place-explore-backend.service, /bin/systemctl status place-explore-backend.service

# 4. 部署目录授权给 deploy 账号可写（或加入 placeapp 组）
sudo usermod -aG placeapp deploy
sudo chmod -R g+w /opt/place-explore/backend/releases /var/www/place-explore/releases
```

## 回滚

```bash
# 查看可回滚的历史版本
bash /opt/place-explore/deploy/rollback.sh list

# 回滚后端到指定版本
bash /opt/place-explore/deploy/rollback.sh backend smart-campus-backend-0.0.1-SNAPSHOT-20260405-091400.jar

# 回滚前端到指定版本
bash /opt/place-explore/deploy/rollback.sh frontend frontend-20260405-01
```

## 安全提醒
- `.env.prod` 与 `application-prod.properties` 中的密钥**不进 Git**，仓库里只保留 `.env.prod.example` 模板
- 2026-07-13 之前 `place-explore-backend.service` 的密钥曾以明文形式出现在 systemd unit 文件及排查记录中，**这些密钥（数据库密码、JWT_SECRET、超级管理员密码）需要轮换**，轮换后同步更新 `.env.prod`
- 部署用 SSH 私钥只保存在 GitHub Secrets，不落地到本地磁盘明文文件

## 首次接入本流程需要做的事（Checklist）
- [ ] 轮换 DB密码 / JWT_SECRET / 超级管理员密码
- [ ] 把 systemd unit 里的 `Environment=` 改为 `EnvironmentFile=/opt/place-explore/backend/.env.prod`
- [ ] 创建 `.env.prod` 并填入（轮换后的）真实密钥，权限设为 600
- [ ] 服务器上创建 `deploy` 账号并配置 SSH key + sudoers（见上）
- [ ] 把 `deploy/` 目录下三个脚本放到 `/opt/place-explore/deploy/` 并 `chmod +x`
- [ ] GitHub 仓库配置好 Secrets: VPS_HOST / VPS_USER / VPS_SSH_KEY
- [ ] 把本文档中 JDK 版本号改成实际使用的版本（`java -version` 在服务器上确认）
- [ ] 测试一次 push 到 main，确认自动部署链路跑通
