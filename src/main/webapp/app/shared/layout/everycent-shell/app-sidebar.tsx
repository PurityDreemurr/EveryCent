import React, { useEffect, useMemo, useState } from 'react';

import NavGroup from './nav-group';
import NavUser from './nav-user';
import { EveryCentNavGroup, everyCentSidebarData } from './nav-config';
import TeamSwitcher from './team-switcher';
import { loadSettings, settingsChangedEvent } from 'app/shared/components/everycent/forms/settings-storage';

type AppSidebarProps = {
  collapsed: boolean;
};

const defaultDisplaySettings = {
  items: ['ai', 'dashboard', 'ledger', 'transactions', 'budget'],
};

const visibleNavGroups = (visibleIds: string[]): EveryCentNavGroup[] => {
  const visibleIdSet = new Set(visibleIds);

  return everyCentSidebarData.navGroups
    .map(group => ({
      ...group,
      items: group.items
        .map(item => {
          if (item.displayId && !visibleIdSet.has(item.displayId)) {
            return null;
          }

          if ('items' in item) {
            const visibleChildren = item.items.filter(child => !child.displayId || visibleIdSet.has(child.displayId));
            return visibleChildren.length > 0 ? { ...item, items: visibleChildren } : null;
          }

          return item;
        })
        .filter(Boolean) as EveryCentNavGroup['items'],
    }))
    .filter(group => group.items.length > 0);
};

const AppSidebar = ({ collapsed }: AppSidebarProps) => {
  const [displayItems, setDisplayItems] = useState(() => loadSettings('display', defaultDisplaySettings).items);
  const navGroups = useMemo(() => visibleNavGroups(displayItems), [displayItems]);

  useEffect(() => {
    const handleSettingsChanged = (event: Event) => {
      const scope = (event as CustomEvent<{ scope?: string }>).detail?.scope;
      if (!scope || scope === 'display') {
        setDisplayItems(loadSettings('display', defaultDisplaySettings).items);
      }
    };

    window.addEventListener(settingsChangedEvent, handleSettingsChanged);
    window.addEventListener('storage', handleSettingsChanged);
    return () => {
      window.removeEventListener(settingsChangedEvent, handleSettingsChanged);
      window.removeEventListener('storage', handleSettingsChanged);
    };
  }, []);

  return (
    <aside className={`everycent-sidebar${collapsed ? ' is-collapsed' : ''}`}>
      <TeamSwitcher collapsed={collapsed} />

      <nav className="everycent-sidebar__nav" aria-label="EveryCent">
        {navGroups.map(group => (
          <NavGroup key={group.title} group={group} collapsed={collapsed} />
        ))}
      </nav>

      <NavUser fallbackUser={everyCentSidebarData.user} collapsed={collapsed} />
    </aside>
  );
};

export default AppSidebar;
