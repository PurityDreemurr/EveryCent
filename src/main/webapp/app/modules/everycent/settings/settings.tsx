import React, { useState } from 'react';

import {
  AccountForm,
  AppearanceForm,
  DisplayForm,
  NotificationsForm,
  ProfileForm,
  TasksDialogs,
  TasksProvider,
  UsersDialogs,
  UsersProvider,
  useTasks,
  useUsers,
} from 'app/shared/components/everycent/forms';

import './settings.scss';

const tabs = [
  { id: 'profile', label: '资料' },
  { id: 'account', label: '账户' },
  { id: 'appearance', label: '外观' },
  { id: 'display', label: '显示' },
  { id: 'notifications', label: '通知' },
] as const;

type SettingsTab = (typeof tabs)[number]['id'];

const sampleUser = {
  firstName: 'Alex',
  lastName: 'Chen',
  username: 'alex_chen',
  email: 'alex@everycent.local',
  phoneNumber: '+8613800000000',
  role: 'manager',
};

const sampleTask = {
  title: 'Review weekly budget warnings',
  status: 'in-progress',
  label: 'feature',
  priority: 'medium',
};

const SettingsDialogActions = () => {
  const users = useUsers();
  const tasks = useTasks();

  const openAddUser = () => users.setOpen('add');
  const openEditUser = () => {
    users.setCurrentRow(sampleUser);
    users.setOpen('edit');
  };
  const openDeleteUser = () => {
    users.setCurrentRow(sampleUser);
    users.setOpen('delete');
  };
  const openCreateTask = () => tasks.setOpen('create');
  const openUpdateTask = () => {
    tasks.setCurrentRow(sampleTask);
    tasks.setOpen('update');
  };
  const openDeleteTask = () => {
    tasks.setCurrentRow(sampleTask);
    tasks.setOpen('delete');
  };

  return (
    <div className="everycent-settings__actions">
      <button className="ec-button" type="button" onClick={openAddUser}>
        添加用户
      </button>
      <button className="ec-button" type="button" onClick={openEditUser}>
        编辑用户
      </button>
      <button className="ec-button" type="button" onClick={openDeleteUser}>
        删除用户
      </button>
      <button className="ec-button ec-button--primary" type="button" onClick={openCreateTask}>
        新建任务
      </button>
      <button className="ec-button" type="button" onClick={openUpdateTask}>
        更新任务
      </button>
      <button className="ec-button" type="button" onClick={openDeleteTask}>
        删除任务
      </button>
    </div>
  );
};

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
        <SettingsDialogActions />
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

      <UsersDialogs />
      <TasksDialogs />
    </div>
  );
};

const Settings = () => (
  <UsersProvider>
    <TasksProvider>
      <SettingsContent />
    </TasksProvider>
  </UsersProvider>
);

export default Settings;
