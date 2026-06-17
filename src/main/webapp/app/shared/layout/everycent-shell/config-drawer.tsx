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
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' },
  { value: 'system', label: '跟随系统' },
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
      <button className="everycent-icon-button" type="button" aria-label="打开布局设置" onClick={openDrawer}>
        <FontAwesomeIcon icon="cogs" />
      </button>
      {open && (
        <div className="everycent-drawer" role="dialog" aria-modal="true" aria-label="主题设置">
          <button className="everycent-drawer__backdrop" type="button" aria-label="关闭设置" onClick={closeDrawer} />
          <aside className="everycent-drawer__panel">
            <header>
              <h2>主题设置</h2>
              <p>调整外观和布局以适配你的工作流。</p>
            </header>
            <section>
              <h3>主题</h3>
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
              <h3>侧边栏</h3>
              <button className="everycent-drawer__wide-button" type="button" onClick={onToggleSidebar}>
                {collapsed ? '展开侧边栏' : '收起侧边栏'}
              </button>
            </section>
            <footer>
              <button type="button" onClick={resetSettings}>
                重置
              </button>
            </footer>
          </aside>
        </div>
      )}
    </div>
  );
};

export default ConfigDrawer;
