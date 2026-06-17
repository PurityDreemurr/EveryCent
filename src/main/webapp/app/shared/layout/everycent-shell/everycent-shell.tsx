import './everycent-theme.scss';
import './everycent-shell.scss';

import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';

import AppSidebar from './app-sidebar';
import { ThemeProvider } from './theme-provider';
import Topbar from './topbar';

const EveryCentShell = () => {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <ThemeProvider>
      <div className={`everycent-shell${collapsed ? ' everycent-shell--collapsed' : ''}`}>
        <AppSidebar collapsed={collapsed} />
        <main className="everycent-shell__main">
          <Topbar collapsed={collapsed} onToggleSidebar={() => setCollapsed(value => !value)} />
          <section className="everycent-shell__content">
            <Outlet />
          </section>
        </main>
      </div>
    </ThemeProvider>
  );
};

export default EveryCentShell;
