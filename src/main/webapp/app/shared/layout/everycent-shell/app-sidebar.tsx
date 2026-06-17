import React from 'react';

import NavGroup from './nav-group';
import NavUser from './nav-user';
import { everyCentSidebarData } from './nav-config';
import TeamSwitcher from './team-switcher';

type AppSidebarProps = {
  collapsed: boolean;
};

const AppSidebar = ({ collapsed }: AppSidebarProps) => (
  <aside className={`everycent-sidebar${collapsed ? ' is-collapsed' : ''}`}>
    <TeamSwitcher teams={everyCentSidebarData.teams} collapsed={collapsed} />

    <nav className="everycent-sidebar__nav" aria-label="EveryCent">
      {everyCentSidebarData.navGroups.map(group => (
        <NavGroup key={group.title} group={group} collapsed={collapsed} />
      ))}
    </nav>

    <NavUser fallbackUser={everyCentSidebarData.user} collapsed={collapsed} />
  </aside>
);

export default AppSidebar;
