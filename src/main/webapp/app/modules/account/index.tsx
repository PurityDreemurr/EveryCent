import React from 'react';
import { Route } from 'react-router';

import EveryCentShell from 'app/shared/layout/everycent-shell/everycent-shell';
import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import Settings from './settings/settings';
import Password from './password/password';

const AccountRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route element={<EveryCentShell />}>
      <Route path="settings" element={<Settings />} />
      <Route path="password" element={<Password />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default AccountRoutes;
