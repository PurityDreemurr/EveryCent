import React from 'react';
import { Navigate, Route } from 'react-router';

import EveryCentShell from 'app/shared/layout/everycent-shell/everycent-shell';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import AiHomePage from './ai-record/ai-home-page';
import Dashboard from './dashboard/dashboard';
import Settings from './settings/settings';

const ComingSoon = ({ title }: { title: string }) => (
  <div className="everycent-page">
    <div className="everycent-page__header">
      <div>
        <h1 className="everycent-page__title">{title}</h1>
        <p className="everycent-page__subtitle">The page shell is ready. Tables, forms, and APIs can be connected next.</p>
      </div>
    </div>
    <div className="everycent-placeholder">Coming soon</div>
  </div>
);

const EveryCentRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route element={<EveryCentShell />}>
      <Route index element={<Navigate to="ai" replace />} />
      <Route path="ai" element={<AiHomePage />} />
      <Route path="dashboard" element={<Dashboard />} />
      <Route path="ledgers" element={<ComingSoon title="Ledgers" />} />
      <Route path="transactions" element={<ComingSoon title="Transactions" />} />
      <Route path="budgets" element={<ComingSoon title="Budgets" />} />
      <Route path="members" element={<ComingSoon title="Members" />} />
      <Route path="analytics" element={<ComingSoon title="Analytics" />} />
      <Route path="notifications" element={<ComingSoon title="Notifications" />} />
      <Route path="export" element={<ComingSoon title="Export Excel" />} />
      <Route path="tags" element={<ComingSoon title="Tags" />} />
      <Route path="settings" element={<Settings />} />
      <Route path="*" element={<Navigate to="ai" replace />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default EveryCentRoutes;
