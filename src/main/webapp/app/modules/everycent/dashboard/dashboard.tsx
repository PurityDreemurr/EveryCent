import './dashboard.scss';

import React, { useState } from 'react';
import { Area, AreaChart, Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from 'recharts';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

type TabKey = 'overview' | 'analytics';

type SummaryCard = {
  label: string;
  value: string;
  change: string;
  icon: IconProp;
  tone: string;
};

const summaryCards: SummaryCard[] = [
  { label: 'Total Income', value: '12,000.00', change: '+20.1% from last month', icon: 'save', tone: 'success' },
  { label: 'Total Expense', value: '3,248.60', change: '+8.4% from last month', icon: 'list', tone: 'danger' },
  { label: 'Budget Left', value: '1,751.40', change: '35% remaining this month', icon: 'tasks', tone: 'warning' },
  { label: 'Net Balance', value: '8,751.40', change: '+12.8% from last month', icon: 'tachometer-alt', tone: 'accent' },
];

const overviewData = [
  { name: 'Jan', total: 4200 },
  { name: 'Feb', total: 3600 },
  { name: 'Mar', total: 5100 },
  { name: 'Apr', total: 4700 },
  { name: 'May', total: 5900 },
  { name: 'Jun', total: 4300 },
  { name: 'Jul', total: 6200 },
  { name: 'Aug', total: 5400 },
  { name: 'Sep', total: 4800 },
  { name: 'Oct', total: 6900 },
  { name: 'Nov', total: 6500 },
  { name: 'Dec', total: 7200 },
];

const analyticsData = [
  { name: 'Mon', income: 900, expense: 520 },
  { name: 'Tue', income: 720, expense: 430 },
  { name: 'Wed', income: 1080, expense: 610 },
  { name: 'Thu', income: 840, expense: 760 },
  { name: 'Fri', income: 1240, expense: 680 },
  { name: 'Sat', income: 680, expense: 520 },
  { name: 'Sun', income: 960, expense: 390 },
];

const recentRecords = [
  { name: 'Lunch', email: 'food / calm', amount: '-28.00', tone: 'expense' },
  { name: 'Metro', email: 'transport / routine', amount: '-6.00', tone: 'expense' },
  { name: 'Salary', email: 'income / work', amount: '+12,000.00', tone: 'income' },
  { name: 'Coffee', email: 'food / relaxed', amount: '-18.00', tone: 'expense' },
  { name: 'Book', email: 'learning / happy', amount: '-56.00', tone: 'expense' },
];

const referrers = [
  { name: 'Dining', value: 512 },
  { name: 'Shopping', value: 238 },
  { name: 'Transport', value: 174 },
  { name: 'Housing', value: 104 },
];

const devices = [
  { name: 'Manual Entry', value: 74 },
  { name: 'AI Parsed', value: 22 },
  { name: 'Import', value: 4 },
];

const DashboardCard = ({
  title,
  description,
  children,
  className = '',
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
}) => (
  <article className={`everycent-dashboard-card ${className}`}>
    <header className="everycent-dashboard-card__header">
      <h2>{title}</h2>
      {description && <p>{description}</p>}
    </header>
    <div className="everycent-dashboard-card__content">{children}</div>
  </article>
);

const OverviewChart = () => (
  <div className="everycent-chart">
    <ResponsiveContainer width="100%" height={350}>
      <BarChart data={overviewData}>
        <XAxis dataKey="name" stroke="#6f7b8d" fontSize={12} tickLine={false} axisLine={false} />
        <YAxis stroke="#6f7b8d" fontSize={12} tickLine={false} axisLine={false} tickFormatter={value => `${value}`} />
        <Bar dataKey="total" fill="#166f86" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  </div>
);

const AnalyticsChart = () => (
  <div className="everycent-chart">
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={analyticsData}>
        <XAxis dataKey="name" stroke="#6f7b8d" fontSize={12} tickLine={false} axisLine={false} />
        <YAxis stroke="#6f7b8d" fontSize={12} tickLine={false} axisLine={false} />
        <Area type="monotone" dataKey="income" stroke="#166f86" fill="#166f86" fillOpacity={0.16} />
        <Area type="monotone" dataKey="expense" stroke="#b43d4a" fill="#b43d4a" fillOpacity={0.1} />
      </AreaChart>
    </ResponsiveContainer>
  </div>
);

const RecentRecords = () => (
  <div className="everycent-recent-records">
    {recentRecords.map(record => (
      <div key={`${record.name}-${record.amount}`} className="everycent-recent-records__item">
        <span className="everycent-recent-records__avatar">{record.name.slice(0, 2).toUpperCase()}</span>
        <span className="everycent-recent-records__copy">
          <strong>{record.name}</strong>
          <small>{record.email}</small>
        </span>
        <strong className={`everycent-recent-records__amount everycent-recent-records__amount--${record.tone}`}>{record.amount}</strong>
      </div>
    ))}
  </div>
);

const SimpleBarList = ({
  items,
  valueFormatter,
}: {
  items: { name: string; value: number }[];
  valueFormatter: (value: number) => string;
}) => {
  const max = Math.max(...items.map(item => item.value), 1);

  return (
    <ul className="everycent-simple-bars">
      {items.map(item => {
        const width = `${Math.round((item.value / max) * 100)}%`;

        return (
          <li key={item.name}>
            <div className="everycent-simple-bars__row">
              <span>{item.name}</span>
              <strong>{valueFormatter(item.value)}</strong>
            </div>
            <div className="everycent-simple-bars__track">
              <span style={{ width }} />
            </div>
          </li>
        );
      })}
    </ul>
  );
};

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState<TabKey>('overview');

  return (
    <div className="everycent-page everycent-dashboard">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">Dashboard</h1>
          <p className="everycent-page__subtitle">Track income, expenses, budgets, and account activity.</p>
        </div>
        <button type="button" className="everycent-dashboard__download">
          Download
        </button>
      </div>

      <div className="everycent-dashboard__tabs" role="tablist" aria-label="Dashboard tabs">
        <button type="button" className={activeTab === 'overview' ? 'active' : ''} onClick={() => setActiveTab('overview')}>
          Overview
        </button>
        <button type="button" className={activeTab === 'analytics' ? 'active' : ''} onClick={() => setActiveTab('analytics')}>
          Analytics
        </button>
        <button type="button" disabled>
          Reports
        </button>
        <button type="button" disabled>
          Notifications
        </button>
      </div>

      {activeTab === 'overview' && (
        <div className="everycent-dashboard__tab-panel">
          <section className="everycent-dashboard__summary" aria-label="Account summary">
            {summaryCards.map(card => (
              <article key={card.label} className={`everycent-summary-card everycent-summary-card--${card.tone}`}>
                <header>
                  <span>{card.label}</span>
                  <FontAwesomeIcon icon={card.icon} />
                </header>
                <strong>{card.value}</strong>
                <small>{card.change}</small>
              </article>
            ))}
          </section>

          <section className="everycent-dashboard__main-grid">
            <DashboardCard title="Overview" className="everycent-dashboard__span-4">
              <OverviewChart />
            </DashboardCard>
            <DashboardCard
              title="Recent Records"
              description="You recorded 265 entries this month."
              className="everycent-dashboard__span-3"
            >
              <RecentRecords />
            </DashboardCard>
          </section>
        </div>
      )}

      {activeTab === 'analytics' && (
        <div className="everycent-dashboard__tab-panel">
          <DashboardCard title="Finance Overview" description="Weekly income and expense movement.">
            <AnalyticsChart />
          </DashboardCard>

          <section className="everycent-dashboard__summary" aria-label="Analytics summary">
            {[
              { label: 'AI Parsed', value: '1,248', change: '+12.4% vs last week', icon: 'pencil-alt' as IconProp, tone: 'accent' },
              { label: 'Unique Tags', value: '832', change: '+5.8% vs last week', icon: 'flag' as IconProp, tone: 'success' },
              { label: 'Over Budget', value: '42%', change: '-3.2% vs last week', icon: 'tasks' as IconProp, tone: 'warning' },
              { label: 'Avg. Entry Time', value: '3m 24s', change: '+18s vs last week', icon: 'sync' as IconProp, tone: 'danger' },
            ].map(card => (
              <article key={card.label} className={`everycent-summary-card everycent-summary-card--${card.tone}`}>
                <header>
                  <span>{card.label}</span>
                  <FontAwesomeIcon icon={card.icon} />
                </header>
                <strong>{card.value}</strong>
                <small>{card.change}</small>
              </article>
            ))}
          </section>

          <section className="everycent-dashboard__main-grid">
            <DashboardCard title="Categories" description="Top spending categories." className="everycent-dashboard__span-4">
              <SimpleBarList items={referrers} valueFormatter={value => `${value}`} />
            </DashboardCard>
            <DashboardCard title="Entry Sources" description="How records enter EveryCent." className="everycent-dashboard__span-3">
              <SimpleBarList items={devices} valueFormatter={value => `${value}%`} />
            </DashboardCard>
          </section>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
