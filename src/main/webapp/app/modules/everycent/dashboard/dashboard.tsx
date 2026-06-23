import './dashboard.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { Area, AreaChart, Bar, BarChart, ResponsiveContainer, XAxis, YAxis } from 'recharts';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

import { getLedgers, Ledger } from 'app/modules/everycent/ledger/ledger-api';

import EmotionTagBadge from '../shared/emotion-tag-badge';
import {
  DashboardPeriod,
  TagStat,
  TrendPoint,
  getBehaviorTagStats,
  getDashboardSummary,
  getDashboardTrend,
  getEmotionTagStats,
} from './dashboard-api';

type TabKey = 'overview' | 'analytics';

type SummaryCard = {
  label: string;
  value: string;
  change: string;
  icon: IconProp;
  tone: string;
};

const monthlyFallback = Array.from({ length: 12 }, (_, index) => ({
  name: `${index + 1}月`,
  total: 0,
}));

const weekFallback = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'].map(name => ({
  date: name,
  income: '0',
  expense: '0',
}));

const cyclePeriod = (activeTab: TabKey): DashboardPeriod => (activeTab === 'overview' ? 'MONTH' : 'WEEK');

const formatMoney = (value?: string | number) => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 });

const formatPercent = (value?: string | number) => `${(Number(value || 0) * 100).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}%`;

const budgetAlertLevelLabel = (level?: string) => {
  if (level === 'DANGER') {
    return '已超支';
  }
  if (level === 'WARNING') {
    return '接近上限';
  }
  if (level === 'INFO') {
    return '正常';
  }
  if (level === 'NONE') {
    return '暂无预算';
  }
  return level || '暂无预算';
};

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

const OverviewChart = ({ data }: { data: { name: string; total: number }[] }) => (
  <div className="everycent-chart">
    <ResponsiveContainer width="100%" height={360}>
      <BarChart data={data} barCategoryGap="42%">
        <XAxis dataKey="name" stroke="var(--ec-muted)" fontSize={12} tickLine={false} axisLine={false} />
        <YAxis stroke="var(--ec-muted)" fontSize={12} tickLine={false} axisLine={false} tickFormatter={value => `¥${value}`} />
        <Bar dataKey="total" fill="var(--ec-chart-bar, #0f172a)" maxBarSize={44} radius={[6, 6, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  </div>
);

const AnalyticsChart = ({ data }: { data: TrendPoint[] }) => (
  <div className="everycent-chart">
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart data={data}>
        <XAxis dataKey="date" stroke="#6f7b8d" fontSize={12} tickLine={false} axisLine={false} />
        <YAxis stroke="#6f7b8d" fontSize={12} tickLine={false} axisLine={false} />
        <Area type="monotone" dataKey="income" stroke="#166f86" fill="#166f86" fillOpacity={0.16} />
        <Area type="monotone" dataKey="expense" stroke="#b43d4a" fill="#b43d4a" fillOpacity={0.1} />
      </AreaChart>
    </ResponsiveContainer>
  </div>
);

const RecentRecords = ({ items }: { items: { name: string; amount: string }[] }) => (
  <div className="everycent-recent-records">
    {items.map(record => (
      <div key={`${record.name}-${record.amount}`} className="everycent-recent-records__item">
        <span className="everycent-recent-records__avatar">{record.name.slice(0, 2).toUpperCase()}</span>
        <span className="everycent-recent-records__copy">
          <strong>{record.name}</strong>
          <small>账本记录</small>
        </span>
        <strong className="everycent-recent-records__amount">{record.amount}</strong>
      </div>
    ))}
  </div>
);

const SimpleBarList = ({
  items,
  renderLabel,
  valueFormatter,
}: {
  items: TagStat[];
  renderLabel?: (item: TagStat) => React.ReactNode;
  valueFormatter: (value: number) => string;
}) => {
  const max = Math.max(...items.map(item => Number(item.amount || 0)), 1);

  return (
    <ul className="everycent-simple-bars">
      {items.map(item => {
        const value = Number(item.amount || 0);
        const width = `${Math.round((value / max) * 100)}%`;

        return (
          <li key={item.tagId}>
            <div className="everycent-simple-bars__row">
              <span>{renderLabel ? renderLabel(item) : item.tagName}</span>
              <strong>{valueFormatter(value)}</strong>
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
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [ledgerLoading, setLedgerLoading] = useState(true);
  const [ledgerError, setLedgerError] = useState<string | null>(null);
  const [selectedLedgerId, setSelectedLedgerId] = useState<number>();
  const [summary, setSummary] = useState<SummaryCard[]>([]);
  const [overviewData, setOverviewData] = useState<{ name: string; total: number }[]>(monthlyFallback);
  const [trendData, setTrendData] = useState<TrendPoint[]>(weekFallback);
  const [behaviorStats, setBehaviorStats] = useState<TagStat[]>([]);
  const [emotionStats, setEmotionStats] = useState<TagStat[]>([]);
  const [analyticsError, setAnalyticsError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const loadLedgers = async () => {
      setLedgerLoading(true);
      setLedgerError(null);
      try {
        const data = await getLedgers();
        if (mounted) {
          setLedgers(data);
          setSelectedLedgerId(current => current || data[0]?.id);
        }
      } catch (error) {
        if (mounted) {
          setLedgerError('账本数据加载失败');
        }
      } finally {
        if (mounted) {
          setLedgerLoading(false);
        }
      }
    };

    loadLedgers();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!selectedLedgerId) {
      return;
    }

    let mounted = true;

    const loadDashboard = async () => {
      setAnalyticsError(null);
      try {
        const [summaryData, trend, behavior, emotion] = await Promise.all([
          getDashboardSummary(selectedLedgerId, { period: cyclePeriod(activeTab) }),
          getDashboardTrend(selectedLedgerId, {}),
          getBehaviorTagStats(selectedLedgerId, { period: 'ALL' }),
          getEmotionTagStats(selectedLedgerId, { period: 'ALL' }),
        ]);

        if (mounted) {
          setSummary([
            {
              label: '账本数量',
              value: ledgerLoading ? '加载中' : `${ledgers.length}`,
              change: ledgerError || '来自 /api/ledgers',
              icon: 'book' as IconProp,
              tone: 'neutral',
            },
            {
              label: '本期收入',
              value: `¥${formatMoney(summaryData.incomeTotal || summaryData.totalIncome)}`,
              change: `交易 ${summaryData.transactionCount || 0} 笔`,
              icon: 'database' as IconProp,
              tone: 'neutral',
            },
            {
              label: '本期支出',
              value: `¥${formatMoney(summaryData.expenseTotal || summaryData.totalExpense)}`,
              change: `结余 ¥${formatMoney(summaryData.balance)}`,
              icon: 'list' as IconProp,
              tone: 'neutral',
            },
            {
              label: '预算状态',
              value: budgetAlertLevelLabel(summaryData.budgetAlertLevel),
              change: `使用率 ${formatPercent(summaryData.budgetUsedRate ?? summaryData.budgetUsedRatio)}`,
              icon: 'tasks' as IconProp,
              tone: 'neutral',
            },
          ]);

          setOverviewData(
            trend.length > 0
              ? trend.map(item => ({
                  name: item.date.slice(5),
                  total: Number(item.expense || 0) + Number(item.income || 0),
                }))
              : monthlyFallback,
          );
          setTrendData(trend.length > 0 ? trend : weekFallback);
          setBehaviorStats(behavior);
          setEmotionStats(emotion);
        }
      } catch (error) {
        if (mounted) {
          setAnalyticsError('看板数据加载失败');
        }
      }
    };

    loadDashboard();

    return () => {
      mounted = false;
    };
  }, [activeTab, selectedLedgerId, ledgers, ledgerError, ledgerLoading]);

  const selectedLedger = useMemo(() => ledgers.find(ledger => ledger.id === selectedLedgerId), [ledgers, selectedLedgerId]);

  const recentRecords = useMemo(
    () =>
      trendData.slice(0, 5).map((item, index) => ({
        name: item.date,
        amount: `¥${formatMoney(index % 2 === 0 ? item.income : item.expense)}`,
      })),
    [trendData],
  );

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
        </div>
      </div>

      {ledgerError && <div className="everycent-dashboard__alert">{ledgerError}</div>}
      {analyticsError && <div className="everycent-dashboard__alert">{analyticsError}</div>}

      <section className="everycent-panel everycent-dashboard__toolbar">
        <label>
          <span>当前账本</span>
          <select
            value={selectedLedgerId || ''}
            onChange={event => setSelectedLedgerId(event.target.value ? Number(event.target.value) : undefined)}
          >
            {ledgerLoading ? (
              <option value="">账本加载中</option>
            ) : (
              ledgers.map(ledger => (
                <option key={ledger.id} value={ledger.id}>
                  {ledger.name}
                </option>
              ))
            )}
          </select>
        </label>
      </section>

      {activeTab === 'overview' && (
        <div className="everycent-dashboard__tab-panel">
          <section className="everycent-dashboard__summary" aria-label="账户摘要">
            {summary.map(card => (
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
              <OverviewChart data={overviewData} />
            </DashboardCard>
            <DashboardCard title="最近记录" description={selectedLedger?.name || '请选择账本'} className="everycent-dashboard__span-3">
              <RecentRecords items={recentRecords} />
            </DashboardCard>
          </section>
        </div>
      )}

      {activeTab === 'analytics' && (
        <div className="everycent-dashboard__tab-panel">
          <DashboardCard title="财务趋势" description="接口返回的趋势数据。">
            <AnalyticsChart data={trendData} />
          </DashboardCard>

          <section className="everycent-dashboard__summary" aria-label="数据分析摘要">
            {[
              {
                label: '行为标签数',
                value: `${behaviorStats.length}`,
                change: '历史所有记录',
                icon: 'flag' as IconProp,
                tone: 'accent',
              },
              {
                label: '情绪标签数',
                value: `${emotionStats.length}`,
                change: '历史所有记录',
                icon: 'heart' as IconProp,
                tone: 'success',
              },
              {
                label: '最大行为标签',
                value: behaviorStats[0]?.tagName || '暂无',
                change: behaviorStats[0] ? `¥${formatMoney(behaviorStats[0].amount)}` : '等待数据',
                icon: 'pencil-alt' as IconProp,
                tone: 'warning',
              },
              {
                label: '最大情绪标签',
                value: emotionStats[0]?.tagName || '暂无',
                content: emotionStats[0] ? <EmotionTagBadge name={emotionStats[0].tagName} /> : null,
                change: emotionStats[0] ? `¥${formatMoney(emotionStats[0].amount)}` : '等待数据',
                icon: 'tasks' as IconProp,
                tone: 'danger',
              },
            ].map(card => (
              <article key={card.label} className={`everycent-summary-card everycent-summary-card--${card.tone}`}>
                <header>
                  <span>{card.label}</span>
                  <FontAwesomeIcon icon={card.icon} />
                </header>
                <strong>{card.content || card.value}</strong>
                <small>{card.change}</small>
              </article>
            ))}
          </section>

          <section className="everycent-dashboard__main-grid">
            <DashboardCard title="行为标签统计" description="历史所有记录" className="everycent-dashboard__span-4">
              <SimpleBarList items={behaviorStats} valueFormatter={value => `¥${value}`} />
            </DashboardCard>
            <DashboardCard title="情绪标签统计" description="历史所有记录" className="everycent-dashboard__span-3">
              <SimpleBarList
                items={emotionStats}
                renderLabel={item => <EmotionTagBadge name={item.tagName} size="sm" />}
                valueFormatter={value => `¥${value}`}
              />
            </DashboardCard>
          </section>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
