# EveryCent 前端业务模块

本目录用于放置 EveryCent 自定义业务页面，保留 JHipster 原有 `modules/account`、`modules/login`、`modules/administration` 等目录不变。

建议子目录：

- `dashboard/`：数据看板。
- `ledger/`：账本管理。
- `transaction/`：收支记录。
- `budget/`：预算管理。
- `ai-record/`：自然语言记账。
- `shared-ledger/`：共享账本和权限。

后续接入路由时，应在 JHipster 现有 React 路由结构中增量添加，不要移动原有模块。
