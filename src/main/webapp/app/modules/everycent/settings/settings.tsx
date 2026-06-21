import React, { useState } from 'react';

import { AccountForm, AppearanceForm, DisplayForm, NotificationsForm, ProfileForm } from 'app/shared/components/everycent/forms';

import './settings.scss';

const tabs = [
  { id: 'profile', label: '资料' },
  { id: 'account', label: '账户' },
  { id: 'appearance', label: '外观' },
  { id: 'display', label: '显示' },
  { id: 'notifications', label: '通知' },
] as const;

type SettingsTab = (typeof tabs)[number]['id'];

const SettingsContent = () => {
  const [activeTab, setActiveTab] = useState<SettingsTab>('profile');

  const renderForm = () => {
    switch (activeTab) {
      case 'profile':
        return <ProfileForm />;
      case 'account':
        return <AccountForm />;
      case 'appearance':
        return <AppearanceForm />;
      case 'display':
        return <DisplayForm />;
      case 'notifications':
        return <NotificationsForm />;
      default:
        return null;
    }
  };

  return (
    <div className="everycent-page everycent-settings">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">设置</h1>
          <p className="everycent-page__subtitle">表单结构保持不变，仅替换为中文文案。</p>
        </div>
      </div>

      <div className="everycent-settings__layout">
        <nav className="everycent-settings__tabs" aria-label="设置分区">
          {tabs.map(tab => (
            <button key={tab.id} className={activeTab === tab.id ? 'active' : ''} type="button" onClick={() => setActiveTab(tab.id)}>
              {tab.label}
            </button>
          ))}
        </nav>
        <section className="everycent-settings__panel">{renderForm()}</section>
      </div>
    </div>
  );
};

const Settings = () => <SettingsContent />;

export default Settings;
