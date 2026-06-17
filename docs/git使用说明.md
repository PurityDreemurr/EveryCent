# EveryCent 三人协作 Git 开发说明

本文档用于规范 EveryCent 智能记账系统三人协作开发流程，明确 Git 分支管理方式、每位成员的代码拉取与更新方法、提交规范，以及不同成员主要负责修改的目录范围。

EveryCent 项目基于 JHipster 生成，采用 Spring Boot + React + MySQL + JWT 的单体前后端集成结构。因此，本项目不拆分为独立的 `frontend/` 和 `backend/` 仓库，而是在 JHipster 原有目录结构下按模块分工开发。

---

## 1. 分支整体设计

项目采用以下分支结构：

```text
main
dev
feature/frontend
feature/backend
feature/database-llm
```

各分支含义如下：

| 分支名                    | 作用                      | 是否直接开发  |
| ---------------------- | ----------------------- | ------- |
| `main`                 | 最终稳定展示版本，用于课程验收、答辩展示    | 不直接开发   |
| `dev`                  | 团队集成开发分支，所有功能开发完成后合并到这里 | 不建议直接开发 |
| `feature/frontend`     | 前端同学开发分支                | 是       |
| `feature/backend`      | 后端同学开发分支                | 是       |
| `feature/database-llm` | 数据库与 LLM 同学开发分支         | 是       |

基本原则：

```text
个人功能分支开发 → 合并到 dev → dev 测试稳定 → 合并到 main
```

禁止三个人都直接在 `main` 或 `dev` 上开发，以免互相覆盖代码或导致主分支不可运行。

---

## 2. 三人分工说明

### 2.1 前端同学

负责内容：

1. React 页面开发。
2. 智能记账首页。
3. 自然语言记账页面。
4. 账本页面。
5. 收支记录页面。
6. 预算管理页面。
7. 数据看板页面。
8. 账本共享页面。
9. 前端接口调用与页面交互。

主要修改目录：

```text
src/main/webapp/app/
src/main/webapp/app/modules/everycent/
src/main/webapp/app/modules/everycent/dashboard/
src/main/webapp/app/modules/everycent/ledger/
src/main/webapp/app/modules/everycent/transaction/
src/main/webapp/app/modules/everycent/budget/
src/main/webapp/app/modules/everycent/ai-record/
src/main/webapp/app/modules/everycent/shared-ledger/
```

可以修改但需要谨慎的目录：

```text
src/main/webapp/app/shared/
src/main/webapp/app/config/
src/main/webapp/app/routes.tsx
```

不建议前端同学随意修改：

```text
src/main/java/
src/main/resources/config/liquibase/
pom.xml
package.json
```

除非与后端同学确认。

对应分支：

```text
feature/frontend
```

---

### 2.2 后端同学

负责内容：

1. Spring Boot 后端接口。
2. 用户登录后的业务权限校验。
3. 账本管理接口。
4. 收支记录接口。
5. 预算管理接口。
6. 数据看板统计接口。
7. Excel 导出接口。
8. 后端 Service 业务逻辑。
9. Repository 查询方法。
10. 与前端约定 API 返回格式。

主要修改目录：

```text
src/main/java/com/everycent/web/rest/
src/main/java/com/everycent/service/
src/main/java/com/everycent/service/dto/
src/main/java/com/everycent/service/mapper/
src/main/java/com/everycent/repository/
src/main/java/com/everycent/domain/
```

可以修改但需要谨慎的目录：

```text
src/main/resources/config/application-dev.yml
src/main/resources/config/application-prod.yml
src/main/resources/config/liquibase/
```

不建议后端同学随意修改：

```text
src/main/webapp/app/
package.json
```

除非与前端同学确认。

对应分支：

```text
feature/backend
```

---

### 2.3 数据库与 LLM 同学

负责内容：

1. 数据库实体设计。
2. Liquibase 数据库变更脚本。
3. 测试数据。
4. 数据完整性约束。
5. 触发器设计。
6. LLM 自然语言记账解析模块。
7. 行为标签识别。
8. 情绪标签识别。
9. 个性化预算提醒文案生成。
10. AI 风险控制与降级策略。
11. 项目文档维护。

主要修改目录：

```text
src/main/resources/config/liquibase/
src/main/resources/config/liquibase/changelog/
src/main/java/com/everycent/llm/
src/main/java/com/everycent/llm/dto/
src/main/java/com/everycent/llm/client/
src/main/java/com/everycent/llm/prompt/
docs/
.env.example
```

可以修改但需要谨慎的目录：

```text
src/main/java/com/everycent/domain/
src/main/java/com/everycent/service/
src/main/java/com/everycent/web/rest/
```

不建议数据库与 LLM 同学随意修改：

```text
src/main/webapp/app/
package.json
```

除非与前端同学确认。

对应分支：

```text
feature/database-llm
```

---

## 3. 第一次克隆项目

每位成员第一次加入项目时，执行：

```bash
git clone git@github.com:你的用户名/你的仓库名.git
cd 你的仓库名
```

如果使用 HTTPS：

```bash
git clone https://github.com/你的用户名/你的仓库名.git
cd 你的仓库名
```

查看远程分支：

```bash
git branch -r
```

应能看到类似：

```text
origin/main
origin/dev
origin/feature/frontend
origin/feature/backend
origin/feature/database-llm
```

---

## 4. 每个人切换到自己的开发分支

### 4.1 前端同学

```bash
git checkout -b feature/frontend origin/feature/frontend
```

如果本地已经有该分支：

```bash
git checkout feature/frontend
```

---

### 4.2 后端同学

```bash
git checkout -b feature/backend origin/feature/backend
```

如果本地已经有该分支：

```bash
git checkout feature/backend
```

---

### 4.3 数据库与 LLM 同学

```bash
git checkout -b feature/database-llm origin/feature/database-llm
```

如果本地已经有该分支：

```bash
git checkout feature/database-llm
```

---

## 5. 每天开始开发前的更新流程

每个人每天开始开发前，都应该先同步 `dev` 分支的最新代码，再合并到自己的功能分支。

### 5.1 更新本地 dev 分支

```bash
git checkout dev
git pull origin dev
```

### 5.2 切回自己的功能分支

前端同学：

```bash
git checkout feature/frontend
```

后端同学：

```bash
git checkout feature/backend
```

数据库与 LLM 同学：

```bash
git checkout feature/database-llm
```

### 5.3 将 dev 最新内容合并到自己的分支

```bash
git merge dev
```

如果没有冲突，就可以继续开发。

如果出现冲突，需要先解决冲突，再执行：

```bash
git add .
git commit -m "fix: resolve merge conflicts with dev"
```

---

## 6. 日常开发提交流程

开发过程中，先查看当前修改状态：

```bash
git status
```

查看具体修改内容：

```bash
git diff
```

确认无误后添加文件：

```bash
git add .
```

提交代码：

```bash
git commit -m "feat: add ledger list page"
```

推送到自己的远程分支：

```bash
git push
```

如果是第一次推送该分支，使用：

```bash
git push -u origin 当前分支名
```

例如：

```bash
git push -u origin feature/frontend
```

---

## 7. Commit 信息规范

建议使用以下格式：

```text
类型: 修改内容
```

常见类型：

| 类型         | 含义       | 示例                                     |
| ---------- | -------- | -------------------------------------- |
| `feat`     | 新功能      | `feat: add transaction create page`    |
| `fix`      | 修复问题     | `fix: fix ledger permission check`     |
| `docs`     | 文档修改     | `docs: update git collaboration guide` |
| `style`    | 样式修改     | `style: adjust dashboard layout`       |
| `refactor` | 代码重构     | `refactor: simplify budget service`    |
| `test`     | 测试相关     | `test: add budget overflow test`       |
| `chore`    | 构建、配置、杂项 | `chore: update gitignore`              |

示例：

```bash
git commit -m "feat: add natural language transaction form"
git commit -m "fix: correct monthly budget query"
git commit -m "docs: add database design document"
```

---

## 8. 合并到 dev 的流程

个人功能开发完成后，不要直接把自己的分支强行 push 到 `dev`。推荐通过 GitHub Pull Request 合并。

### 8.1 推送个人分支

例如前端同学：

```bash
git checkout feature/frontend
git push
```

### 8.2 在 GitHub 创建 Pull Request

进入 GitHub 仓库页面，点击：

```text
Compare & pull request
```

选择：

```text
base: dev
compare: feature/frontend
```

后端同学对应：

```text
base: dev
compare: feature/backend
```

数据库与 LLM 同学对应：

```text
base: dev
compare: feature/database-llm
```

### 8.3 合并前检查

合并前至少确认：

1. 项目能正常启动。
2. 没有提交 `.env`、数据库密码、API Key。
3. 没有误删 JHipster 生成文件。
4. 没有修改其他成员负责的大量文件。
5. 没有明显冲突。
6. README 或 docs 中的重要说明同步更新。

---

## 9. dev 合并到 main

当 `dev` 分支经过测试，确认可以用于阶段展示或最终答辩时，再合并到 `main`。

由组长或负责人操作：

```bash
git checkout main
git pull origin main
git merge dev
git push origin main
```

也可以在 GitHub 上创建 Pull Request：

```text
base: main
compare: dev
```

`main` 分支应始终保持稳定，不应包含未完成、无法运行的代码。

---

## 10. 推荐的开发目录边界

为了减少冲突，三个人应尽量只修改自己负责的目录。

### 10.1 前端开发范围

```text
src/main/webapp/app/modules/everycent/
src/main/webapp/app/shared/
src/main/webapp/app/routes.tsx
src/main/webapp/app/app.tsx
```

前端同学如果需要新增接口调用，可以在前端目录中封装 API 请求，但接口路径和返回格式需要提前与后端同学确认。

---

### 10.2 后端开发范围

```text
src/main/java/com/everycent/domain/
src/main/java/com/everycent/repository/
src/main/java/com/everycent/service/
src/main/java/com/everycent/service/dto/
src/main/java/com/everycent/service/mapper/
src/main/java/com/everycent/web/rest/
```

后端同学如果需要新增数据库字段，应先与数据库同学确认 Liquibase 变更脚本。

---

### 10.3 数据库与 LLM 开发范围

```text
src/main/resources/config/liquibase/
src/main/resources/config/liquibase/changelog/
src/main/java/com/everycent/llm/
docs/
.env.example
```

数据库同学需要注意：已经合并到 `dev` 的 Liquibase changelog 不要随意修改。如果后续要改表结构，应新增新的 changelog 文件，而不是直接改旧文件。

---

## 11. 数据库文件协作规范

JHipster 使用 Liquibase 管理数据库结构。数据库相关修改应放在：

```text
src/main/resources/config/liquibase/changelog/
```

建议命名方式：

```text
YYYYMMDDHHMMSS_added_entity_Ledger.xml
YYYYMMDDHHMMSS_added_entity_Transaction.xml
YYYYMMDDHHMMSS_added_budget_constraints.xml
YYYYMMDDHHMMSS_added_trigger_update_balance.xml
```

规则：

1. 不要直接手动删除已经提交的 changelog。
2. 不要随意修改已经在其他人数据库上执行过的 changelog。
3. 表结构变更应新增 changelog。
4. 测试数据应单独放置，并在文档中说明用途。
5. 重要字段需要在 `docs/database-design.md` 中同步说明。

---

## 12. LLM 模块协作规范

LLM 模块建议放在：

```text
src/main/java/com/everycent/llm/
```

推荐结构：

```text
src/main/java/com/everycent/llm/
├── client/
│   └── LlmClient.java
├── dto/
│   └── TransactionParseResult.java
├── prompt/
│   └── PromptTemplate.java
├── LlmPromptService.java
└── LlmParsingService.java
```

LLM 模块必须遵守以下规则：

1. 不得把真实 API Key 写入代码。
2. API Key 应通过环境变量读取。
3. `.env` 不允许提交。
4. 只能提交 `.env.example`。
5. LLM 返回结果不能直接入库，必须由后端校验。
6. 金额、类型、标签、时间等字段必须做合法性检查。
7. LLM 调用失败时，系统应保留传统表单记账方式。

---

## 13. 不允许提交的内容

以下文件或内容不允许提交到 GitHub：

```text
.env
*.env
真实 API Key
数据库真实密码
个人本地配置
IDE 缓存
node_modules/
target/
日志文件
临时测试文件
```

提交前可以检查：

```bash
git status
```

如果发现敏感文件已经被加入暂存区，可以取消：

```bash
git restore --staged 文件名
```

例如：

```bash
git restore --staged .env
```

---

## 14. 常见问题处理

### 14.1 本地分支落后于远程分支

```bash
git pull
```

如果仍然有问题：

```bash
git fetch origin
git status
```

---

### 14.2 合并 dev 时出现冲突

先执行：

```bash
git status
```

打开冲突文件，查找：

```text
<<<<<<< HEAD
当前分支内容
=======
dev 分支内容
>>>>>>> dev
```

手动保留正确内容后：

```bash
git add .
git commit -m "fix: resolve merge conflicts"
```

---

### 14.3 提交到了错误分支

先查看当前分支：

```bash
git branch
```

如果误提交到了 `dev`，不要直接 push，先联系组员或组长处理。

---

### 14.4 想撤销未提交修改

撤销单个文件：

```bash
git restore 文件名
```

撤销所有未提交修改：

```bash
git restore .
```

注意：该操作会丢失本地修改，执行前要确认。

---

### 14.5 想查看最近提交记录

```bash
git log --oneline --graph --decorate --all
```

---

## 15. 推荐的每日协作流程

每位成员每天开发建议按以下流程执行：

```bash
# 1. 切到 dev 并同步最新代码
git checkout dev
git pull origin dev

# 2. 切回自己的功能分支
git checkout feature/你的分支名

# 3. 合并 dev 最新内容
git merge dev

# 4. 开始开发
# 修改代码...

# 5. 查看修改
git status
git diff

# 6. 提交
git add .
git commit -m "feat: your change description"

# 7. 推送到远程个人分支
git push
```

开发完成后，在 GitHub 上创建 Pull Request：

```text
base: dev
compare: feature/你的分支名
```

---

## 16. 三人常用命令速查

### 前端同学

```bash
git checkout dev
git pull origin dev
git checkout feature/frontend
git merge dev

# 开发完成后
git add .
git commit -m "feat: add frontend page"
git push
```

### 后端同学

```bash
git checkout dev
git pull origin dev
git checkout feature/backend
git merge dev

# 开发完成后
git add .
git commit -m "feat: add backend api"
git push
```

### 数据库与 LLM 同学

```bash
git checkout dev
git pull origin dev
git checkout feature/database-llm
git merge dev

# 开发完成后
git add .
git commit -m "feat: add database and llm module"
git push
```

---

## 17. 最终提交前检查清单

合并到 `dev` 或 `main` 前，建议检查：

```bash
git status
```

确保没有遗漏文件。

检查是否误提交敏感信息：

```bash
git diff --cached
```

确认项目基础功能：

```text
1. 后端可以启动。
2. 前端可以访问。
3. 登录功能正常。
4. 自己负责的模块基本可用。
5. 没有破坏其他成员模块。
6. README 和 docs 已同步更新。
```

---

## 18. 总结

EveryCent 项目的协作原则是：

```text
main 保持稳定
dev 用于集成
feature 分支用于个人开发
每个人只修改自己负责的主要目录
通过 Pull Request 合并
数据库变更通过 Liquibase 管理
LLM 密钥不进入 GitHub
```

按照该流程协作，可以减少代码冲突，保证项目在课程设计开发过程中始终保持清晰、可追踪、可回滚。
