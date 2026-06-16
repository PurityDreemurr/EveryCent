# EveryCent Docker 外部数据库运行指南

## 1. 目标架构

本配置只将 EveryCent 前后端放入 Docker 容器运行，不在 Docker 中运行数据库。

- 前端：由生产构建产物打包进 Spring Boot，容器内由 Spring Boot 统一提供访问。
- 后端：Spring Boot/JHipster 应用运行在同一个应用容器中。
- 数据库：运行在现实物理服务器上的 MySQL，通过 `DB_HOST:DB_PORT` 访问。

## 2. 已新增或调整的文件

- `Dockerfile`：使用 Docker 完成 Maven + 前端生产构建，并生成运行镜像。
- `.dockerignore`：减少 Docker 构建上下文。
- `src/main/docker/app.yml`：只启动 EveryCent 应用容器，不再启动 MySQL 容器，并使用宿主机网络模式。
- `src/main/docker/everycent.env.example`：外部数据库和运行参数模板。

## 3. 物理 MySQL 准备

在物理 MySQL 服务器上确认 MySQL 监听真实网卡地址，而不是只监听 `127.0.0.1`。

建议数据库与账号：

```sql
CREATE DATABASE IF NOT EXISTS everycent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'everycent'@'%' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON everycent.* TO 'everycent'@'%';
FLUSH PRIVILEGES;
```

如果只允许应用服务器访问，建议将 `%` 改成 Docker 宿主机的 IP。

同时确认防火墙允许 Docker 宿主机访问 MySQL 端口，例如 `3306`。

## 4. 创建运行配置

复制模板：

```bash
cp src/main/docker/everycent.env.example src/main/docker/everycent.env
```

编辑：

```bash
nano src/main/docker/everycent.env
```

`src/main/docker/everycent.env` 已被 `.gitignore` 和 `.dockerignore` 排除，不会提交到 Git，也不会进入 Docker 构建上下文。

必须修改：

```properties
APP_BASE_URL=http://你的应用服务器IP:8080
DB_HOST=你的物理数据库服务器IP
DB_PORT=3306
DB_NAME=everycent
DB_USERNAME=everycent
DB_PASSWORD=你的数据库密码
JWT_BASE64_SECRET=你的JWT密钥
APP_LLM_API_KEY=你的阿里云API Key
```

生成 JWT 密钥：

```bash
openssl rand -base64 64
```

注意：容器内的 `localhost` 指容器自己，不是物理数据库服务器。因此 `DB_HOST` 必须填写数据库服务器真实 IP，不能写 `localhost`。

## 5. 构建镜像

在项目根目录执行：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml build
```

该命令会在 Docker 构建阶段完成：

- Maven 后端构建
- npm 依赖安装
- 前端生产构建
- Spring Boot 可运行 Jar 打包
- 生成 `everycent:latest` 镜像

## 6. 启动容器

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml up -d
```

查看状态：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml ps
```

查看日志：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml logs -f app
```

## 7. 访问系统

浏览器访问：

```text
http://你的应用服务器IP:8080
```

当前 Docker Compose 使用：

```yaml
network_mode: host
```

这表示容器直接使用宿主机网络，不再维护 `ports:` 端口映射。开发阶段如果修改 `APP_PORT`，例如：

```properties
APP_PORT=9000
APP_BASE_URL=http://你的应用服务器IP:9000
```

重启后直接访问：

```text
http://你的应用服务器IP:9000
```

不需要额外修改 Docker 端口映射。

健康检查：

```bash
curl http://你的应用服务器IP:8080/management/health
```

如果修改了 `APP_PORT`，健康检查地址中的端口也要同步修改。

注意：`network_mode: host` 适合 Linux 开发服务器。Docker Desktop for macOS/Windows 对 host 网络模式的行为可能不同。

## 8. 停止、重启与更新

停止：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml down
```

重启：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml restart app
```

代码更新后重新构建并启动：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml build
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/app.yml up -d
```

## 9. 数据库连通性排查

在 Docker 宿主机上先确认能访问物理数据库：

```bash
mysql -h 你的物理数据库服务器IP -P 3306 -u everycent -p everycent -e "SELECT 1;"
```

如果宿主机可以访问，但容器不能访问，检查：

- `DB_HOST` 是否写成了 `localhost`。
- 物理数据库防火墙是否只允许宿主机某个网段。
- MySQL 用户授权 host 是否允许 Docker 宿主机访问。
- 云服务器安全组是否放行数据库端口。

## 10. 说明

当前 `src/main/docker/mysql.yml` 仍保留为 JHipster 原始开发辅助文件，但 `src/main/docker/app.yml` 不再引用它。按本指南运行时，不会启动数据库容器。
