# 三人协作流程

## 分工

- A：负责前端页面、React、TypeScript、表单交互、路由接入和数据看板。
- B：负责后端接口、业务逻辑、权限校验、服务层、REST Controller 和异常处理。
- C：负责数据库设计、Liquibase changelog、LLM 控制器、提示词、AI 结果校验和降级策略。

## 分支规范

- `main`：稳定展示版本，只合并已验证的阶段成果。
- `dev`：集成开发分支，所有功能分支先合并到这里。
- `feature/frontend`：前端业务页面开发。
- `feature/backend`：后端接口和业务逻辑开发。
- `feature/database-llm`：数据库设计、Liquibase 和 LLM 模块开发。

## 开发流程

1. 从 `dev` 拉取最新代码。
2. 基于 `dev` 创建自己的功能分支。
3. 在本地完成开发和自测。
4. 提交清晰的 commit，说明本次修改的业务范围。
5. push 到远程仓库。
6. 提 Pull Request 合并到 `dev`。
7. 至少由另一名同学检查代码、接口文档或页面效果。
8. 阶段稳定后由组内统一将 `dev` 合并到 `main`。

## 协作注意事项

- 不要提交 `.env`、真实 API Key、数据库密码或 JWT 密钥。
- 不要修改已合并的历史 Liquibase changelog，应新增 changelog。
- 前后端接口字段变更时，同步更新 `docs/api-design.md`。
- 数据库结构变更时，同步更新 `docs/database-design.md`。
- LLM 提示词、输出格式和校验规则变更时，同步更新 `docs/llm-design.md`。
