import React from 'react';
import { Navigate, Route } from 'react-router';

import EveryCentShell from 'app/shared/layout/everycent-shell/everycent-shell';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import AiHomePage from './ai-record/ai-home-page';
import Dashboard from './dashboard/dashboard';
import ExportPage from './export/export';
import LedgerPage from './ledger/ledger';
import BudgetsPage from './budget/budgets';
import NotificationsPage from './notifications/notifications';
import Settings from './settings/settings';
import MembersPage from './shared-ledger/members';
import TransactionsPage from './transaction/transactions';
import TagsPage from './tags/tags';

const ComingSoon = ({ title }: { title: string }) => (
  <div className="everycent-page">
    <div className="everycent-page__header">
      <div>
        <h1 className="everycent-page__title">{title}</h1>
        <p className="everycent-page__subtitle">页面骨架已就绪，后续可继续接入列表、表单和接口。</p>
      </div>
    </div>
    <div className="everycent-placeholder">待接入</div>
  </div>
);

const EveryCentRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route element={<EveryCentShell />}>
      <Route index element={<Navigate to="/everycent/ai" replace />} />
      <Route path="ai" element={<AiHomePage />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="ledgers" element={<LedgerPage />} />
      <Route path="transactions" element={<TransactionsPage />} />
      <Route path="budgets" element={<BudgetsPage />} />
      <Route path="members" element={<MembersPage />} />
      <Route path="analytics" element={<ComingSoon title="数据分析" />} />
      <Route path="notifications" element={<NotificationsPage />} />
      <Route path="export" element={<ExportPage />} />
      <Route path="tags" element={<TagsPage />} />
      <Route path="settings" element={<Settings />} />
      <Route path="help" element={<ComingSoon title="帮助中心" />} />
      <Route path="*" element={<Navigate to="/everycent/ai" replace />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default EveryCentRoutes;
