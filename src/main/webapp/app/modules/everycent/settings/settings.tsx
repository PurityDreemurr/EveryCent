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
  { id: 'profile', label: 'Profile' },
  { id: 'account', label: 'Account' },
  { id: 'appearance', label: 'Appearance' },
  { id: 'display', label: 'Display' },
  { id: 'notifications', label: 'Notifications' },
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
        Add user
      </button>
      <button className="ec-button" type="button" onClick={openEditUser}>
        Edit user
      </button>
      <button className="ec-button" type="button" onClick={openDeleteUser}>
        Delete user
      </button>
      <button className="ec-button ec-button--primary" type="button" onClick={openCreateTask}>
        Create task
      </button>
      <button className="ec-button" type="button" onClick={openUpdateTask}>
        Update task
      </button>
      <button className="ec-button" type="button" onClick={openDeleteTask}>
        Delete task
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
          <h1 className="everycent-page__title">Settings</h1>
          <p className="everycent-page__subtitle">Form patterns adapted from shadcn-admin for EveryCent.</p>
        </div>
        <SettingsDialogActions />
      </div>

      <div className="everycent-settings__layout">
        <nav className="everycent-settings__tabs" aria-label="Settings sections">
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
