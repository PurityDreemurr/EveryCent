# EveryCent Docker 实时开发运行指南

## 1. 适用场景

这套配置用于开发阶段：

- 修改宿主机代码后，容器内立即可见。
- 不需要每次修改代码后重新构建 Docker 镜像。
- 可以进入容器，通过 CLI 启动、停止、重启后端和前端。
- 数据库仍然使用物理服务器 MySQL，不放入 Docker。

## 2. 文件说明

- `Dockerfile.dev`：开发环境镜像，包含 Node.js 22、npm、JDK 17、git、curl。
- `src/main/docker/docker-dev.yml`：开发容器 Compose 配置，使用 `network_mode: host` 和源码挂载。
- `src/main/docker/everycent.env`：复用外部数据库、JWT、LLM 等配置。

开发容器使用 `DEV_SPRING_PROFILES_ACTIVE`，默认值是：

```properties
DEV_SPRING_PROFILES_ACTIVE=dev,api-docs
```

这样可以避免复用生产容器配置时误用 `SPRING_PROFILES_ACTIVE=prod,api-docs`。

## 3. 启动开发容器

先确认已经有运行配置：

```bash
cp src/main/docker/everycent.env.example src/main/docker/everycent.env
nano src/main/docker/everycent.env
```

启动开发容器：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml up -d --build
```

如果构建过程中 apt 下载失败，可以清理缓存后重新构建：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml build --no-cache
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml up -d
```

开发镜像已使用阿里云 Debian 镜像源，并只安装 `openjdk-17-jdk-headless`，避免下载完整桌面相关 JDK 依赖。

容器内前端监听使用 polling，默认间隔为 `1000ms`。如果文件变更响应太慢或 CPU 占用较高，可以在 `src/main/docker/everycent.env` 中调整：

```properties
WEBPACK_POLL_INTERVAL=1000
CHOKIDAR_INTERVAL=1000
```

进入容器：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml exec dev bash
```

## 4. 首次安装依赖

进入容器后执行：

```bash
npm install
```

Maven 依赖会在第一次启动后端时自动下载到 Docker volume 中。

## 5. 启动后端

在容器终端 1 中执行：

```bash
./mvnw -Dskip.installnodenpm -Dskip.npm -ntp
```

后端默认监听 `APP_PORT`，通常是 `8080`。

如果要手动重启后端：

1. 在该终端按 `Ctrl+C`。
2. 重新执行：

```bash
./mvnw -Dskip.installnodenpm -Dskip.npm -ntp
```

## 6. 启动前端

另开一个终端，进入同一个开发容器：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml exec dev bash
```

执行：

```bash
npm start
```

前端开发服务：

- BrowserSync 页面：`http://你的服务器IP:9000`
- Webpack Dev Server：`http://你的服务器IP:9060`

通常使用：

```text
http://你的服务器IP:9000
```

不要用 `http://你的服务器IP:8080` 访问实时开发前端。`8080` 是 Spring Boot 后端端口，如果后端没有打包过前端静态资源，会显示 JHipster 的 “An error has occurred :-(” 占位错误页。实时开发时应访问 `9000`，由 BrowserSync/webpack-dev-server 提供前端页面，并自动代理 `/api`、`/management` 等请求到后端 `8080`。

## 7. 实时修改效果

前端：

- 修改 `src/main/webapp` 下的 React/TS/CSS 文件后，Webpack 会自动重新编译。
- 浏览器通常会自动刷新。

后端：

- 修改 Java 代码后，最稳定的方式是手动重启后端 CLI。
- 如果 IDE 或 Maven 已经把修改编译到 `target/classes`，Spring DevTools 可能自动重启应用。
- 当前配置优先保证“容器内 CLI 可重启”，不强制依赖 IDE 自动编译。

## 8. 一条命令同时启动前后端

如果想在同一个容器终端同时启动前后端，可以执行：

```bash
npm run watch
```

该命令会同时运行前端开发服务和后端服务。缺点是前后端日志会混在一起；调试时更推荐分两个终端分别启动。

## 9. 常用命令

查看容器状态：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml ps
```

停止开发容器：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml down
```

清理开发容器和依赖缓存 volume：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml down -v
```

仅当 `Dockerfile.dev` 改了，才需要重建开发镜像：

```bash
docker compose --env-file src/main/docker/everycent.env -f src/main/docker/docker-dev.yml build
```

## 10. 端口说明

开发容器使用：

```yaml
network_mode: host
```

因此容器内监听的端口会直接暴露到宿主机：

- 后端：`APP_PORT`，默认 `8080`
- BrowserSync：`9000`
- Webpack Dev Server：`9060`

不需要在 Docker Compose 中手动维护端口映射。

实时开发推荐访问：

```text
http://你的服务器IP:9000
```

只有在使用生产镜像或已经执行过前端生产构建并由后端托管静态资源时，才访问后端端口 `8080`。
