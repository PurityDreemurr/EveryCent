import './budgets.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { getLedgers, Ledger } from 'app/modules/everycent/ledger/ledger-api';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from 'app/shared/components/everycent/overlays/dialog';

import {
  Budget,
  BudgetCycle,
  BudgetPayload,
  budgetLimit,
  budgetUsageRate,
  createBudget,
  deleteBudget,
  getBudgetStatus,
  getBudgets,
  updateBudget,
} from './budget-api';

type DialogMode = 'create' | 'edit' | 'status';

type BudgetForm = {
  id?: number;
  cycle: BudgetCycle;
  periodStart: string;
  periodEnd: string;
  limitAmount: string;
  alertThreshold: string;
  enabled: boolean;
};

const emptyForm: BudgetForm = {
  cycle: 'MONTHLY',
  periodStart: new Date().toISOString().slice(0, 10),
  periodEnd: new Date().toISOString().slice(0, 10),
  limitAmount: '',
  alertThreshold: '0.80',
  enabled: true,
};

const cycleLabel = (cycle?: BudgetCycle) => {
  if (cycle === 'WEEKLY') {
    return '每周';
  }
  if (cycle === 'MONTHLY') {
    return '每月';
  }
  return cycle || '未知';
};

const statusLabel = (status?: string) => {
  if (status === 'DANGER') {
    return '超支';
  }
  if (status === 'WARNING') {
    return '预警';
  }
  if (status === 'INFO') {
    return '正常';
  }
  return status || '未知';
};

const BudgetsPage = () => {
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [selectedLedgerId, setSelectedLedgerId] = useState<number>();
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [loadingLedgers, setLoadingLedgers] = useState(true);
  const [loadingBudgets, setLoadingBudgets] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<DialogMode>('create');
  const [form, setForm] = useState<BudgetForm>(emptyForm);
  const [statusBudget, setStatusBudget] = useState<Budget | null>(null);

  const selectedLedger = useMemo(() => ledgers.find(ledger => ledger.id === selectedLedgerId), [ledgers, selectedLedgerId]);

  const loadBudgets = async (ledgerId: number) => {
    setLoadingBudgets(true);
    setError(null);
    try {
      const data = await getBudgets(ledgerId);
      setBudgets(data);
    } catch (err) {
      setError('预算列表加载失败，请检查后端接口。');
    } finally {
      setLoadingBudgets(false);
    }
  };

  useEffect(() => {
    let mounted = true;

    const loadLedgers = async () => {
      setLoadingLedgers(true);
      try {
        const data = await getLedgers();
        if (mounted) {
          setLedgers(data);
          const firstLedgerId = data[0]?.id;
          setSelectedLedgerId(current => current || firstLedgerId);
          if (firstLedgerId) {
            await loadBudgets(firstLedgerId);
          }
        }
      } catch (err) {
        if (mounted) {
          setError('账本列表加载失败。');
        }
      } finally {
        if (mounted) {
          setLoadingLedgers(false);
        }
      }
    };

    loadLedgers();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (selectedLedgerId) {
      loadBudgets(selectedLedgerId);
    } else {
      setBudgets([]);
    }
  }, [selectedLedgerId]);

  const openCreate = () => {
    setForm(emptyForm);
    setDialogMode('create');
    setStatusBudget(null);
    setDialogOpen(true);
  };

  const openEdit = (budget: Budget) => {
    setForm({
      id: budget.id,
      cycle: budget.cycle,
      periodStart: budget.periodStart,
      periodEnd: budget.periodEnd,
      limitAmount: budgetLimit(budget),
      alertThreshold: budget.alertThreshold || '0.80',
      enabled: budget.enabled,
    });
    setDialogMode('edit');
    setStatusBudget(null);
    setDialogOpen(true);
  };

  const openStatus = async (budget: Budget) => {
    if (!selectedLedgerId) {
      return;
    }
    setDialogMode('status');
    setError(null);
    try {
      const status = await getBudgetStatus(selectedLedgerId, { cycle: budget.cycle, date: budget.periodStart });
      setStatusBudget(status);
      setDialogOpen(true);
    } catch (err) {
      setError('预算状态加载失败。');
    }
  };

  const payloadFromForm = (current: BudgetForm): BudgetPayload => ({
    cycle: current.cycle,
    periodStart: current.periodStart,
    periodEnd: current.periodEnd,
    limitAmount: current.limitAmount.trim(),
    alertThreshold: current.alertThreshold || '0.80',
    enabled: current.enabled,
  });

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedLedgerId) {
      setError('请先选择账本。');
      return;
    }

    if (!form.limitAmount.trim()) {
      setError('请填写预算金额。');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = payloadFromForm(form);
      if (dialogMode === 'edit' && form.id) {
        await updateBudget(form.id, payload);
      } else {
        await createBudget(selectedLedgerId, payload);
      }
      setDialogOpen(false);
      await loadBudgets(selectedLedgerId);
    } catch (err) {
      setError(dialogMode === 'edit' ? '修改预算失败。' : '创建预算失败。');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (budget: Budget) => {
    const confirmed = window.confirm(`确定删除这个${cycleLabel(budget.cycle)}预算吗？`);
    if (!confirmed) {
      return;
    }

    setError(null);
    try {
      await deleteBudget(budget.id);
      if (selectedLedgerId) {
        await loadBudgets(selectedLedgerId);
      }
    } catch (err) {
      setError('删除预算失败。');
    }
  };

  return (
    <div className="everycent-page everycent-budgets-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">预算管理</h1>
          <p className="everycent-page__subtitle">查看、创建、修改和删除账本预算，并随时查看预算状态。</p>
        </div>
        <button type="button" className="everycent-budgets-page__primary" onClick={openCreate} disabled={!selectedLedgerId}>
          <FontAwesomeIcon icon="plus" />
          新建预算
        </button>
      </div>

      {error && <div className="everycent-budgets-page__alert">{error}</div>}

      <section className="everycent-panel everycent-budgets-page__toolbar">
        <label>
          <span>当前账本</span>
          <select
            value={selectedLedgerId || ''}
            onChange={event => setSelectedLedgerId(event.target.value ? Number(event.target.value) : undefined)}
            disabled={loadingLedgers || ledgers.length === 0}
          >
            {loadingLedgers ? (
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

      <section className="everycent-panel everycent-budgets-page__list">
        <header className="everycent-budgets-page__section-header">
          <div>
            <h2>{selectedLedger?.name || '未选择账本'}</h2>
            <span>共 {budgets.length} 个预算</span>
          </div>
        </header>

        {loadingBudgets ? (
          <div className="everycent-budgets-page__empty">正在读取预算...</div>
        ) : budgets.length === 0 ? (
          <div className="everycent-budgets-page__empty">当前账本还没有预算。</div>
        ) : (
          <div className="everycent-budgets-page__items">
            {budgets.map(budget => (
              <article key={budget.id} className="everycent-budgets-page__item">
                <div>
                  <strong>{cycleLabel(budget.cycle)}</strong>
                  <span>
                    {budget.periodStart} - {budget.periodEnd}
                  </span>
                </div>
                <div>
                  <em>预算金额</em>
                  <strong>{budgetLimit(budget)}</strong>
                </div>
                <div>
                  <em>已使用</em>
                  <strong>{budget.usedAmount || '0'}</strong>
                </div>
                <div>
                  <em>使用率</em>
                  <strong>{budgetUsageRate(budget)}</strong>
                </div>
                <div>
                  <em>状态</em>
                  <strong>{statusLabel(budget.status)}</strong>
                </div>
                <div className="everycent-budgets-page__item-actions">
                  <button type="button" onClick={() => openStatus(budget)}>
                    状态
                  </button>
                  <button type="button" onClick={() => openEdit(budget)}>
                    编辑
                  </button>
                  <button type="button" className="danger" onClick={() => handleDelete(budget)}>
                    删除
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="everycent-budget-dialog">
          <DialogHeader>
            <DialogTitle>
              {dialogMode === 'create' && '新建预算'}
              {dialogMode === 'edit' && '编辑预算'}
              {dialogMode === 'status' && '预算状态'}
            </DialogTitle>
            <DialogDescription>{dialogMode === 'status' ? '查看预算当前使用情况。' : '按账本配置预算周期和金额。'}</DialogDescription>
          </DialogHeader>

          {dialogMode === 'status' && statusBudget ? (
            <div className="everycent-budgets-page__status-grid">
              <div>
                <span>预算金额</span>
                <strong>{budgetLimit(statusBudget)}</strong>
              </div>
              <div>
                <span>已使用</span>
                <strong>{statusBudget.usedAmount || '0'}</strong>
              </div>
              <div>
                <span>剩余</span>
                <strong>{statusBudget.remainingAmount || '0'}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{statusLabel(statusBudget.status)}</strong>
              </div>
            </div>
          ) : (
            <form className="everycent-budgets-page__form" onSubmit={handleSubmit}>
              <label>
                <span>周期</span>
                <select
                  value={form.cycle}
                  onChange={event => setForm(current => ({ ...current, cycle: event.target.value as BudgetCycle }))}
                >
                  <option value="MONTHLY">每月</option>
                  <option value="WEEKLY">每周</option>
                </select>
              </label>
              <label>
                <span>预算金额</span>
                <input value={form.limitAmount} onChange={event => setForm(current => ({ ...current, limitAmount: event.target.value }))} />
              </label>
              <label>
                <span>开始日期</span>
                <input
                  type="date"
                  value={form.periodStart}
                  onChange={event => setForm(current => ({ ...current, periodStart: event.target.value }))}
                />
              </label>
              <label>
                <span>结束日期</span>
                <input
                  type="date"
                  value={form.periodEnd}
                  onChange={event => setForm(current => ({ ...current, periodEnd: event.target.value }))}
                />
              </label>
              <label>
                <span>提醒阈值</span>
                <input
                  value={form.alertThreshold}
                  onChange={event => setForm(current => ({ ...current, alertThreshold: event.target.value }))}
                />
              </label>
              <label>
                <span>启用</span>
                <select
                  value={String(form.enabled)}
                  onChange={event => setForm(current => ({ ...current, enabled: event.target.value === 'true' }))}
                >
                  <option value="true">启用</option>
                  <option value="false">停用</option>
                </select>
              </label>
              <div className="everycent-budgets-page__form-actions">
                <button type="button" onClick={() => setDialogOpen(false)}>
                  取消
                </button>
                <button type="submit" className="primary" disabled={saving}>
                  {saving ? '保存中...' : '保存预算'}
                </button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default BudgetsPage;
