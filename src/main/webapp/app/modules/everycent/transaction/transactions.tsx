import './transactions.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { getLedgers, Ledger } from 'app/modules/everycent/ledger/ledger-api';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from 'app/shared/components/everycent/overlays/dialog';

import {
  createTransaction,
  deleteTransaction,
  getTransaction,
  getTransactions,
  TransactionPayload,
  TransactionQuery,
  TransactionRecord,
  updateTransaction,
} from './transaction-api';

type DialogMode = 'create' | 'edit' | 'detail';

type TransactionForm = {
  id?: number;
  type: string;
  amount: string;
  description: string;
  recordDate: string;
  behaviorTagId: string;
  emotionTagId: string;
  source: string;
};

const today = new Date().toISOString().slice(0, 10);

const emptyForm: TransactionForm = {
  type: 'EXPENSE',
  amount: '',
  description: '',
  recordDate: today,
  behaviorTagId: '1',
  emotionTagId: '1',
  source: 'MANUAL',
};

const typeOptions = [
  { value: '', label: '全部类型' },
  { value: 'EXPENSE', label: '支出' },
  { value: 'INCOME', label: '收入' },
];

const transactionTypeLabel = (type?: string) => {
  if (type === 'EXPENSE') {
    return '支出';
  }
  if (type === 'INCOME') {
    return '收入';
  }
  return type || '未知';
};

const DateFilterInput = ({ value, onChange }: { value?: string; onChange: (value: string) => void }) => {
  const inputRef = React.useRef<HTMLInputElement>(null);

  const openPicker = () => {
    const input = inputRef.current;
    if (!input) {
      return;
    }

    if (typeof input.showPicker === 'function') {
      input.showPicker();
      return;
    }

    input.click();
  };

  return (
    <div className="everycent-date-filter">
      <button type="button" className={`everycent-date-filter__button${value ? '' : ' is-empty'}`} onClick={openPicker}>
        <span>{value || ''}</span>
        <FontAwesomeIcon icon="calendar-alt" />
      </button>
      <input
        ref={inputRef}
        className="everycent-date-filter__native"
        type="date"
        value={value || ''}
        tabIndex={-1}
        aria-hidden="true"
        onChange={event => onChange(event.target.value)}
      />
    </div>
  );
};

const toForm = (record: TransactionRecord): TransactionForm => ({
  id: record.id,
  type: record.type,
  amount: record.amount,
  description: record.description,
  recordDate: record.recordDate,
  behaviorTagId: String(record.behaviorTagId || ''),
  emotionTagId: String(record.emotionTagId || ''),
  source: record.source || 'MANUAL',
});

const toPayload = (form: TransactionForm): TransactionPayload => ({
  type: form.type,
  amount: form.amount.trim(),
  description: form.description.trim(),
  recordDate: form.recordDate,
  behaviorTagId: Number(form.behaviorTagId),
  emotionTagId: Number(form.emotionTagId),
  source: form.source || 'MANUAL',
});

const TransactionsPage = () => {
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [selectedLedgerId, setSelectedLedgerId] = useState<number>();
  const [records, setRecords] = useState<TransactionRecord[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [query, setQuery] = useState<TransactionQuery>({ page: 0, size: 20 });
  const [form, setForm] = useState<TransactionForm>(emptyForm);
  const [detailRecord, setDetailRecord] = useState<TransactionRecord | null>(null);
  const [dialogMode, setDialogMode] = useState<DialogMode>('create');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [loadingLedgers, setLoadingLedgers] = useState(true);
  const [loadingRecords, setLoadingRecords] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedLedger = useMemo(() => ledgers.find(ledger => ledger.id === selectedLedgerId), [ledgers, selectedLedgerId]);
  const totalPages = Math.max(Math.ceil(totalElements / size), 1);

  useEffect(() => {
    let mounted = true;

    const loadLedgers = async () => {
      setLoadingLedgers(true);
      setError(null);
      try {
        const data = await getLedgers();
        if (mounted) {
          setLedgers(data);
          setSelectedLedgerId(current => current || data[0]?.id);
        }
      } catch (err) {
        if (mounted) {
          setError('账本列表加载失败，请确认登录状态。');
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
    if (!selectedLedgerId) {
      setRecords([]);
      setTotalElements(0);
      return;
    }

    let mounted = true;

    const loadRecords = async () => {
      setLoadingRecords(true);
      setError(null);
      try {
        const data = await getTransactions(selectedLedgerId, {
          ...query,
          page,
          size,
        });
        if (mounted) {
          setRecords(data.content);
          setTotalElements(data.totalElements);
        }
      } catch (err) {
        if (mounted) {
          setError('收支记录加载失败，请检查筛选条件或账本权限。');
        }
      } finally {
        if (mounted) {
          setLoadingRecords(false);
        }
      }
    };

    loadRecords();

    return () => {
      mounted = false;
    };
  }, [page, query, selectedLedgerId, size]);

  const refreshCurrentPage = async () => {
    if (!selectedLedgerId) {
      return;
    }

    const data = await getTransactions(selectedLedgerId, {
      ...query,
      page,
      size,
    });
    setRecords(data.content);
    setTotalElements(data.totalElements);
  };

  const openCreateDialog = () => {
    setForm(emptyForm);
    setDetailRecord(null);
    setDialogMode('create');
    setDialogOpen(true);
  };

  const openEditDialog = async (record: TransactionRecord) => {
    setDialogMode('edit');
    setDetailRecord(null);
    setError(null);
    try {
      const detail = await getTransaction(record.id);
      setForm(toForm(detail));
      setDialogOpen(true);
    } catch (err) {
      setError('收支记录详情加载失败。');
    }
  };

  const openDetailDialog = async (record: TransactionRecord) => {
    setDialogMode('detail');
    setError(null);
    try {
      const detail = await getTransaction(record.id);
      setDetailRecord(detail);
      setDialogOpen(true);
    } catch (err) {
      setError('收支记录详情加载失败。');
    }
  };

  const handleFilterSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setQuery(current => ({
      ...current,
      page: 0,
    }));
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedLedgerId) {
      setError('请先选择账本。');
      return;
    }

    if (!form.amount.trim() || !form.description.trim() || !form.recordDate || !form.behaviorTagId || !form.emotionTagId) {
      setError('请完整填写金额、描述、日期和标签 ID。');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = toPayload(form);
      if (dialogMode === 'edit' && form.id) {
        await updateTransaction(form.id, payload);
      } else {
        await createTransaction(selectedLedgerId, payload);
      }
      setDialogOpen(false);
      setForm(emptyForm);
      await refreshCurrentPage();
    } catch (err) {
      setError(dialogMode === 'edit' ? '修改收支记录失败。' : '创建收支记录失败。');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (record: TransactionRecord) => {
    const confirmed = window.confirm(`确定删除“${record.description}”这条记录吗？`);
    if (!confirmed) {
      return;
    }

    setError(null);
    try {
      await deleteTransaction(record.id);
      await refreshCurrentPage();
    } catch (err) {
      setError('删除收支记录失败。');
    }
  };

  return (
    <div className="everycent-page everycent-transactions-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">收支记录</h1>
          <p className="everycent-page__subtitle">按账本查看收支流水，支持筛选、新增、编辑和删除。</p>
        </div>
        <button type="button" className="everycent-transactions-page__primary" onClick={openCreateDialog} disabled={!selectedLedgerId}>
          <FontAwesomeIcon icon="plus" />
          新建记录
        </button>
      </div>

      {error && <div className="everycent-transactions-page__alert">{error}</div>}

      <form className="everycent-panel everycent-transactions-page__filters" onSubmit={handleFilterSubmit}>
        <label>
          <span>当前账本</span>
          <select
            value={selectedLedgerId || ''}
            onChange={event => {
              setSelectedLedgerId(event.target.value ? Number(event.target.value) : undefined);
              setPage(0);
            }}
            disabled={loadingLedgers || ledgers.length === 0}
          >
            {loadingLedgers ? (
              <option value="">账本加载中</option>
            ) : ledgers.length === 0 ? (
              <option value="">暂无账本</option>
            ) : (
              ledgers.map(ledger => (
                <option key={ledger.id} value={ledger.id}>
                  {ledger.name}
                </option>
              ))
            )}
          </select>
        </label>
        <label>
          <span>类型</span>
          <select value={query.type || ''} onChange={event => setQuery(current => ({ ...current, type: event.target.value }))}>
            {typeOptions.map(option => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>开始日期</span>
          <DateFilterInput value={query.startDate} onChange={value => setQuery(current => ({ ...current, startDate: value }))} />
        </label>
        <label>
          <span>结束日期</span>
          <DateFilterInput value={query.endDate} onChange={value => setQuery(current => ({ ...current, endDate: value }))} />
        </label>
        <label>
          <span>行为标签 ID</span>
          <input
            type="number"
            min="1"
            value={query.behaviorTagId || ''}
            onChange={event => setQuery(current => ({ ...current, behaviorTagId: event.target.value }))}
          />
        </label>
        <label>
          <span>情绪标签 ID</span>
          <input
            type="number"
            min="1"
            value={query.emotionTagId || ''}
            onChange={event => setQuery(current => ({ ...current, emotionTagId: event.target.value }))}
          />
        </label>
        <div className="everycent-transactions-page__filter-actions">
          <button
            type="button"
            onClick={() => {
              setQuery({ page: 0, size });
              setPage(0);
            }}
          >
            重置
          </button>
          <button type="submit" className="primary">
            查询
          </button>
        </div>
      </form>

      <section className="everycent-panel everycent-transactions-page__table-panel">
        <header className="everycent-transactions-page__section-header">
          <div>
            <h2>{selectedLedger?.name || '未选择账本'}</h2>
            <span>共 {totalElements} 条记录</span>
          </div>
          <button type="button" onClick={() => refreshCurrentPage()} disabled={!selectedLedgerId || loadingRecords}>
            <FontAwesomeIcon icon="sync" />
            刷新
          </button>
        </header>

        {loadingRecords ? (
          <div className="everycent-transactions-page__empty">正在读取收支记录...</div>
        ) : records.length === 0 ? (
          <div className="everycent-transactions-page__empty">当前筛选条件下没有收支记录。</div>
        ) : (
          <div className="everycent-transactions-page__table-wrap">
            <table className="everycent-transactions-page__table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>类型</th>
                  <th>描述</th>
                  <th>行为标签</th>
                  <th>情绪标签</th>
                  <th>来源</th>
                  <th>金额</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {records.map(record => (
                  <tr key={record.id}>
                    <td>{record.recordDate}</td>
                    <td>
                      <em className={`everycent-transactions-page__type everycent-transactions-page__type--${record.type.toLowerCase()}`}>
                        {transactionTypeLabel(record.type)}
                      </em>
                    </td>
                    <td>{record.description}</td>
                    <td>{record.behaviorTagName || record.behaviorTagId}</td>
                    <td>{record.emotionTagName || record.emotionTagId}</td>
                    <td>{record.source}</td>
                    <td>
                      <strong
                        className={`everycent-transactions-page__amount everycent-transactions-page__amount--${record.type.toLowerCase()}`}
                      >
                        {record.type === 'EXPENSE' ? '-' : '+'}¥{Number(record.amount).toLocaleString('zh-CN')}
                      </strong>
                    </td>
                    <td>
                      <div className="everycent-transactions-page__actions">
                        <button type="button" onClick={() => openDetailDialog(record)}>
                          详情
                        </button>
                        <button type="button" onClick={() => openEditDialog(record)}>
                          编辑
                        </button>
                        <button type="button" className="danger" onClick={() => handleDelete(record)}>
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <footer className="everycent-transactions-page__pagination">
          <span>
            第 {page + 1} / {totalPages} 页
          </span>
          <div>
            <button type="button" disabled={page <= 0} onClick={() => setPage(current => Math.max(current - 1, 0))}>
              上一页
            </button>
            <button type="button" disabled={page + 1 >= totalPages} onClick={() => setPage(current => current + 1)}>
              下一页
            </button>
          </div>
        </footer>
      </section>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="everycent-transaction-dialog">
          <DialogHeader>
            <DialogTitle>
              {dialogMode === 'create' && '新建收支记录'}
              {dialogMode === 'edit' && '编辑收支记录'}
              {dialogMode === 'detail' && '收支记录详情'}
            </DialogTitle>
            <DialogDescription>
              {dialogMode === 'detail' ? '查看这条记录的完整信息。' : '手动录入收支记录，AI 自然语言解析会走独立接口。'}
            </DialogDescription>
          </DialogHeader>

          {dialogMode === 'detail' && detailRecord ? (
            <div className="everycent-transactions-page__detail-grid">
              <div>
                <span>金额</span>
                <strong>¥{Number(detailRecord.amount).toLocaleString('zh-CN')}</strong>
              </div>
              <div>
                <span>类型</span>
                <strong>{transactionTypeLabel(detailRecord.type)}</strong>
              </div>
              <div>
                <span>描述</span>
                <strong>{detailRecord.description}</strong>
              </div>
              <div>
                <span>日期</span>
                <strong>{detailRecord.recordDate}</strong>
              </div>
              <div>
                <span>行为标签</span>
                <strong>{detailRecord.behaviorTagName || detailRecord.behaviorTagId}</strong>
              </div>
              <div>
                <span>情绪标签</span>
                <strong>{detailRecord.emotionTagName || detailRecord.emotionTagId}</strong>
              </div>
              <div>
                <span>来源</span>
                <strong>{detailRecord.source}</strong>
              </div>
              <div>
                <span>记录 ID</span>
                <strong>{detailRecord.id}</strong>
              </div>
            </div>
          ) : (
            <form className="everycent-transactions-page__form" onSubmit={handleSubmit}>
              <label>
                <span>类型</span>
                <select value={form.type} onChange={event => setForm(current => ({ ...current, type: event.target.value }))}>
                  <option value="EXPENSE">支出</option>
                  <option value="INCOME">收入</option>
                </select>
              </label>
              <label>
                <span>金额</span>
                <input value={form.amount} onChange={event => setForm(current => ({ ...current, amount: event.target.value }))} />
              </label>
              <label>
                <span>描述</span>
                <input value={form.description} onChange={event => setForm(current => ({ ...current, description: event.target.value }))} />
              </label>
              <label>
                <span>记录日期</span>
                <input
                  type="date"
                  value={form.recordDate}
                  onChange={event => setForm(current => ({ ...current, recordDate: event.target.value }))}
                />
              </label>
              <label>
                <span>行为标签 ID</span>
                <input
                  type="number"
                  min="1"
                  value={form.behaviorTagId}
                  onChange={event => setForm(current => ({ ...current, behaviorTagId: event.target.value }))}
                />
              </label>
              <label>
                <span>情绪标签 ID</span>
                <input
                  type="number"
                  min="1"
                  value={form.emotionTagId}
                  onChange={event => setForm(current => ({ ...current, emotionTagId: event.target.value }))}
                />
              </label>
              <div className="everycent-transactions-page__form-actions">
                <button type="button" onClick={() => setDialogOpen(false)}>
                  取消
                </button>
                <button type="submit" className="primary" disabled={saving}>
                  {saving ? '保存中...' : '保存记录'}
                </button>
              </div>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default TransactionsPage;
