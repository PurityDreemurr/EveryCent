import React from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import type { EveryCentTheme } from './theme-provider';
import { useEveryCentTheme } from './theme-provider';

type ConfigDrawerProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  collapsed: boolean;
  onToggleSidebar: () => void;
};

const themeOptions: { label: string; value: EveryCentTheme }[] = [
  { value: 'light', label: 'light' },
  { value: 'dark', label: 'dark' },
  { value: 'system', label: 'system' },
];

const ConfigDrawer = ({ open, onOpenChange, collapsed, onToggleSidebar }: ConfigDrawerProps) => {
  const { resetTheme, setTheme, theme } = useEveryCentTheme();

  const openDrawer = () => onOpenChange(true);
  const closeDrawer = () => onOpenChange(false);
  const resetSettings = () => {
    resetTheme();
    if (collapsed) {
      onToggleSidebar();
    }
  };

  return (
    <div className="everycent-topbar__menu-wrap">
      <button className="everycent-icon-button" type="button" aria-label="Open layout settings" onClick={openDrawer}>
        <FontAwesomeIcon icon="cogs" />
      </button>
      {open && (
        <div className="everycent-drawer" role="dialog" aria-modal="true" aria-label="Theme settings">
          <button className="everycent-drawer__backdrop" type="button" aria-label="Close settings" onClick={closeDrawer} />
          <aside className="everycent-drawer__panel">
            <header>
              <h2>Theme Settings</h2>
              <p>Adjust the appearance and layout to suit your workflow.</p>
            </header>
            <section>
              <h3>Theme</h3>
              <div className="everycent-drawer__options">
                {themeOptions.map(option => (
                  <button
                    key={option.value}
                    className={theme === option.value ? 'active' : ''}
                    type="button"
                    onClick={() => setTheme(option.value)}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
            </section>
            <section>
              <h3>Sidebar</h3>
              <button className="everycent-drawer__wide-button" type="button" onClick={onToggleSidebar}>
                {collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              </button>
            </section>
            <footer>
              <button type="button" onClick={resetSettings}>
                Reset
              </button>
            </footer>
          </aside>
        </div>
      )}
    </div>
  );
};

export default ConfigDrawer;
