# EveryCent 智能记账系统

EveryCent 是一个基于 JHipster 生成的课程设计项目，主题是“具有 AI 辅助功能的智能记账系统”。项目保留 JHipster 默认的 Spring Boot + React 单体结构，在此基础上扩展账本、收支记录、预算管理、数据看板、共享账本、Excel 导出、定时提醒和 LLM 自然语言记账能力。

## 技术栈

- 后端：Spring Boot / JHipster / Spring Security / JWT / JPA / Liquibase
- 前端：React / TypeScript / JHipster Webapp
- 数据库：MySQL
- AI：LLM API，用于自然语言消费解析、行为标签识别、情绪标签识别和个性化预算提醒

## 本地开发启动

后端启动：

```bash
./mvnw
```

前端开发启动：

```bash
npm start
```

开发访问地址：

- 前端开发服务：http://localhost:9000
- 后端服务：http://localhost:8080

默认测试账号：

- 管理员：`admin/admin`
- 普通用户：`user/user`

如依赖发生变化，可按 JHipster 默认方式安装前端依赖：

```bash
./npmw install
```

## 构建与测试

运行后端与前端测试：

```bash
./mvnw verify
./npmw test
```

生产环境打包为 jar：

```bash
./mvnw -Pprod clean verify
java -jar target/*.jar
```

生产环境打包为 war：

```bash
./mvnw -Pprod,war clean verify
```

## 三人分工

- A：负责前端页面、React 组件、路由、表单交互和数据看板图表。
- B：负责后端接口、业务逻辑、权限校验、服务层和 REST Controller。
- C：负责数据库设计、Liquibase changelog、LLM 控制器、提示词和 AI 结果校验。

## Git 分支协作规范

- `main`：稳定展示版本，只合并阶段性可演示成果。
- `dev`：集成开发分支，日常功能分支合并目标。
- `feature/frontend`：前端页面和交互开发。
- `feature/backend`：后端接口和业务逻辑开发。
- `feature/database-llm`：数据库、Liquibase 和 LLM 模块开发。

推荐流程：从 `dev` 拉取功能分支，本地开发并提交 commit，push 到远程后发起 Pull Request 合并到 `dev`；阶段稳定后再由 `dev` 合并到 `main`。

## 目录结构说明

本项目不拆分为独立的 `frontend/` 和 `backend/`，而是保留 JHipster 原有单体工程结构：

- `src/main/java/com/everycent/`：后端 Java 源码，包含配置、安全、用户、REST 接口和业务服务。
- `src/main/resources/`：后端配置、国际化、日志和 Liquibase 配置。
- `src/main/webapp/app/`：React + TypeScript 前端源码。
- `src/main/resources/config/liquibase/`：数据库版本管理目录。
- `docs/`：课程设计说明文档。
- `src/main/java/com/everycent/llm/`：EveryCent 新增 LLM 模块占位目录。
- `src/main/webapp/app/modules/everycent/`：EveryCent 自定义业务前端页面目录。

更详细的目录说明见 [docs/project-structure.md](docs/project-structure.md)。

## 注意事项

- 不要提交 `.env`、真实 LLM API Key、数据库密码、JWT 密钥等敏感信息。
- 只提交 `.env.example` 作为环境变量示例。
- 数据库表结构变更应新增 Liquibase changelog，不要直接修改已经合并的历史 changelog。
- 不要删除 JHipster 自动生成的认证、安全、配置、测试和构建代码。

## 后续开发计划

1. 设计 Ledger、Transaction、Budget、LedgerPermission 等实体和 Liquibase changelog。
2. 实现账本、收支记录、预算、共享账本和数据看板 REST API。
3. 实现自然语言记账解析接口，并对 LLM 输出做后端校验。
4. 完成 React 业务页面，包括看板、账本、记账、预算、AI 录入和共享账本。
5. 增加 Excel 导出、预算超支告警和定时提醒。
6. 补充单元测试、集成测试和课程设计演示数据。
