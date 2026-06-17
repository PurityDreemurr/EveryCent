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
  { label: '总收入', value: '$45,231.89', change: '较上月 +20.1%', icon: 'dollar-sign', tone: 'neutral' },
  { label: '订阅数', value: '+2350', change: '较上月 +180.1%', icon: 'users', tone: 'neutral' },
  { label: '销售额', value: '+12,234', change: '较上月 +19%', icon: 'save', tone: 'neutral' },
  { label: '当前活跃', value: '+573', change: '较上小时 +201', icon: 'wave-square', tone: 'neutral' },
];

const overviewData = [
  { name: '1月', total: 3650 },
  { name: '2月', total: 2850 },
  { name: '3月', total: 1100 },
  { name: '4月', total: 5800 },
  { name: '5月', total: 1900 },
  { name: '6月', total: 6000 },
  { name: '7月', total: 5450 },
  { name: '8月', total: 5700 },
  { name: '9月', total: 2000 },
  { name: '10月', total: 1100 },
  { name: '11月', total: 6050 },
  { name: '12月', total: 3900 },
];

const analyticsData = [
  { name: '周一', income: 900, expense: 520 },
  { name: '周二', income: 720, expense: 430 },
  { name: '周三', income: 1080, expense: 610 },
  { name: '周四', income: 840, expense: 760 },
  { name: '周五', income: 1240, expense: 680 },
  { name: '周六', income: 680, expense: 520 },
  { name: '周日', income: 960, expense: 390 },
];

const recentRecords = [
  { name: '刘晨', email: 'liuchen@email.com', amount: '+$1,999.00', tone: 'income' },
  { name: '张译', email: 'zhangyi@email.com', amount: '+$39.00', tone: 'income' },
  { name: '李宁', email: 'lining@email.com', amount: '+$299.00', tone: 'income' },
  { name: '王凯', email: 'wangkai@email.com', amount: '+$99.00', tone: 'income' },
  { name: '陈雪', email: 'chenxue@email.com', amount: '+$39.00', tone: 'income' },
];

const referrers = [
  { name: '餐饮', value: 512 },
  { name: '购物', value: 238 },
  { name: '交通', value: 174 },
  { name: '住房', value: 104 },
];

const devices = [
  { name: '手动录入', value: 74 },
  { name: 'AI 解析', value: 22 },
  { name: '导入', value: 4 },
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
    <ResponsiveContainer width="100%" height={360}>
      <BarChart data={overviewData}>
        <XAxis dataKey="name" stroke="var(--ec-muted)" fontSize={12} tickLine={false} axisLine={false} />
        <YAxis stroke="var(--ec-muted)" fontSize={12} tickLine={false} axisLine={false} tickFormatter={value => `$${value}`} />
        <Bar dataKey="total" fill="var(--ec-chart-bar, #0f172a)" radius={[6, 6, 0, 0]} />
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
      <div className="everycent-dashboard__header">
        <div>
          <h1 className="everycent-dashboard__title">仪表盘</h1>
        </div>
        <button type="button" className="everycent-dashboard__download">
          下载
        </button>
      </div>

      <div className="everycent-dashboard__tabs-row">
        <div className="everycent-dashboard__tabs" role="tablist" aria-label="仪表盘标签页">
          <button type="button" className={activeTab === 'overview' ? 'active' : ''} onClick={() => setActiveTab('overview')}>
            概览
          </button>
          <button type="button" className={activeTab === 'analytics' ? 'active' : ''} onClick={() => setActiveTab('analytics')}>
            数据分析
          </button>
          <button type="button" disabled>
            报表
          </button>
          <button type="button" disabled>
            通知
          </button>
        </div>
      </div>

      {activeTab === 'overview' && (
        <div className="everycent-dashboard__tab-panel">
          <section className="everycent-dashboard__summary" aria-label="账户摘要">
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
            <DashboardCard title="概览" className="everycent-dashboard__span-4">
              <OverviewChart />
            </DashboardCard>
            <DashboardCard title="最近记录" description="本月已记录 265 条。" className="everycent-dashboard__span-3">
              <RecentRecords />
            </DashboardCard>
          </section>
        </div>
      )}

      {activeTab === 'analytics' && (
        <div className="everycent-dashboard__tab-panel">
          <DashboardCard title="财务概览" description="每周收入与支出变化。">
            <AnalyticsChart />
          </DashboardCard>

          <section className="everycent-dashboard__summary" aria-label="数据分析摘要">
            {[
              { label: 'AI 解析', value: '1,248', change: '较上周 +12.4%', icon: 'pencil-alt' as IconProp, tone: 'accent' },
              { label: '唯一标签', value: '832', change: '较上周 +5.8%', icon: 'flag' as IconProp, tone: 'success' },
              { label: '超预算', value: '42%', change: '较上周 -3.2%', icon: 'tasks' as IconProp, tone: 'warning' },
              { label: '平均录入时间', value: '3m 24s', change: '较上周 +18s', icon: 'sync' as IconProp, tone: 'danger' },
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
            <DashboardCard title="分类" description="支出最高的分类。" className="everycent-dashboard__span-4">
              <SimpleBarList items={referrers} valueFormatter={value => `${value}`} />
            </DashboardCard>
            <DashboardCard title="录入来源" description="记录进入 EveryCent 的方式。" className="everycent-dashboard__span-3">
              <SimpleBarList items={devices} valueFormatter={value => `${value}%`} />
            </DashboardCard>
          </section>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
