# 项目结构说明

EveryCent 是基于 JHipster 生成的 Spring Boot + React 单体项目。项目保留 JHipster 原始目录和构建方式，只在原有结构内新增课程设计文档、LLM 模块占位包和业务前端页面目录。

## JHipster 原生结构

### `src/main/java/com/everycent/`

后端 Java 主包，基础包名为 `com.everycent`。当前已有 JHipster 生成的应用入口、配置、安全、用户管理、REST 接口、Service、Repository 和领域模型。

常见子目录：

- `config/`：Spring、缓存、安全、Liquibase、Web 等配置。
- `security/`：认证、授权、JWT 相关安全代码。
- `domain/`：JPA 实体。
- `repository/`：Spring Data JPA Repository。
- `service/`：业务服务和 DTO。
- `web/rest/`：REST Controller。

这些目录属于 JHipster 核心结构，不应删除或随意移动。

### `src/main/webapp/app/`

前端 React + TypeScript 源码目录。JHipster 已生成登录、注册、账户管理、后台管理、通用布局、Redux store、路由和共享组件。

常见子目录：

- `modules/`：登录、账户、管理后台等页面模块。
- `shared/`：通用布局、权限路由、错误页、工具函数和共享模型。
- `config/`：前端 store、axios 拦截器、日志和常量配置。
- `entities/`：JHipster entity 子生成器生成的实体页面入口。

EveryCent 的业务页面应放在 `src/main/webapp/app/modules/everycent/`，避免和 JHipster 原生模块混杂。

### `src/main/resources/config/liquibase/`

数据库版本管理目录。JHipster 使用 Liquibase 管理数据库 schema 和初始化数据。

- `master.xml`：Liquibase 总入口，负责 include 各个 changelog。
- `changelog/`：数据库结构变更文件。
- `data/`：初始化数据，例如用户和权限 CSV。

数据库同学后续应通过新增 changelog 管理表结构，并在 `master.xml` 中 include。不要直接修改已经合并且被其他成员使用过的历史 changelog。

## EveryCent 新增结构

### `docs/`

课程设计说明文档目录，用于记录项目结构、协作流程、接口规划、数据库设计、LLM 设计和测试计划。

### `docs/assets/`

文档资源目录，后续可放系统架构图、E-R 图、数据流图、操作截图等。

### `src/main/java/com/everycent/llm/`

EveryCent 新增 LLM 模块占位包，后续用于自然语言记账、标签识别和 AI 预算提醒。

建议后续扩展为：

- `client/`：LLM API 调用客户端。
- `prompt/`：提示词模板和组装逻辑。
- `dto/`：请求、响应和解析结果 DTO。
- `service/`：解析、校验和降级业务逻辑。
- `web/rest/` 或现有 `web/rest/`：对外 REST Controller。

当前项目先提供 `LlmClient`、`LlmPromptService`、`LlmParsingService` 和 `dto/TransactionParseResult` 作为骨架。

### `src/main/webapp/app/modules/everycent/`

EveryCent 自定义业务前端页面目录。建议按照业务域拆分：

- `dashboard/`：收入、支出、余额和分类占比看板。
- `ledger/`：账本列表、账本详情和账本设置。
- `transaction/`：收支记录列表、传统表单记账。
- `budget/`：每周、每月预算管理。
- `ai-record/`：自然语言记账入口。
- `shared-ledger/`：共享账本、权限和监督视图。

## 为什么不拆分为 `frontend/ backend/ database/ llm/`

JHipster 生成的是一个完整单体工程，Maven、Webpack、测试、Spring Boot 配置、Liquibase、认证安全和前端资源都已经围绕现有目录协同工作。强行拆成独立 `frontend/`、`backend/`、`database/`、`llm/` 会导致：

- Maven 和前端构建脚本失效或需要大范围改造。
- JHipster 自动生成的认证、权限、用户管理和前端路由难以复用。
- Liquibase、测试和 Docker 配置路径需要重新维护。
- 课程设计开发成本增加，容易引入与业务无关的构建问题。

因此本项目采用“保留 JHipster 原结构，在约定包和模块下扩展业务”的方式。
