import React, { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import CommandMenu from './command-menu';
import ConfigDrawer from './config-drawer';
import ProfileDropdown from './profile-dropdown';
import ThemeSwitch from './theme-switch';
import TopNav, { TopNavLink } from './top-nav';

type TopbarProps = {
  collapsed: boolean;
  onToggleSidebar: () => void;
};

const topNavLinks: TopNavLink[] = [
  { title: '概览', href: '/everycent/dashboard' },
  { title: 'AI 记账', href: '/everycent/ai' },
  { title: '收支记录', href: '/everycent/transactions' },
  { title: '预算', href: '/everycent/budgets' },
];

const Topbar = ({ collapsed, onToggleSidebar }: TopbarProps) => {
  const location = useLocation();
  const [commandOpen, setCommandOpen] = useState(false);
  const [configOpen, setConfigOpen] = useState(false);
  const showTopNav = location.pathname === '/everycent/dashboard';

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        setCommandOpen(true);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  return (
    <header className={`everycent-topbar${showTopNav ? ' everycent-topbar--with-nav' : ''}`}>
      <button
        className="everycent-icon-button"
        type="button"
        aria-label={collapsed ? '展开侧边栏' : '收起侧边栏'}
        onClick={onToggleSidebar}
        title={collapsed ? '展开侧边栏' : '收起侧边栏'}
      >
        <FontAwesomeIcon icon="th-list" />
      </button>
      <span className="everycent-topbar__separator" />
      {showTopNav ? (
        <TopNav links={topNavLinks} />
      ) : (
        <button className="everycent-search-button everycent-search-button--grow" type="button" onClick={() => setCommandOpen(true)}>
          <FontAwesomeIcon icon="search" />
          <span>搜索</span>
          <kbd>Ctrl K</kbd>
        </button>
      )}
      <div className="everycent-topbar__actions">
        {showTopNav && (
          <button className="everycent-search-button" type="button" onClick={() => setCommandOpen(true)}>
            <FontAwesomeIcon icon="search" />
            <span>搜索</span>
            <kbd>Ctrl K</kbd>
          </button>
        )}
        <ThemeSwitch />
        <ConfigDrawer open={configOpen} onOpenChange={setConfigOpen} collapsed={collapsed} onToggleSidebar={onToggleSidebar} />
        <ProfileDropdown />
      </div>
      <CommandMenu open={commandOpen} onOpenChange={setCommandOpen} />
    </header>
  );
};

export default Topbar;
