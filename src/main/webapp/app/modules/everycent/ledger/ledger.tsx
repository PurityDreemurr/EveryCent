import './ledger.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from 'app/shared/components/everycent/overlays/dialog';

import { createLedger, deleteLedger, getLedgers, Ledger, updateLedger } from './ledger-api';

type FormState = {
  id?: number;
  name: string;
  description: string;
};

const emptyForm: FormState = {
  name: '',
  description: '',
};

const formatDate = (value?: string) => {
  if (!value) {
    return '暂无';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date(value));
};

const LedgerPage = () => {
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [selectedId, setSelectedId] = useState<number>();
  const [form, setForm] = useState<FormState>(emptyForm);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<'create' | 'edit'>('create');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedLedger = useMemo(() => ledgers.find(ledger => ledger.id === selectedId) || ledgers[0], [ledgers, selectedId]);

  const loadLedgers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getLedgers();
      setLedgers(data);
      setSelectedId(current => current || data[0]?.id);
    } catch (err) {
      setError('账本列表加载失败，请确认后端服务和登录状态。');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLedgers();
  }, []);

  const startCreate = () => {
    setForm(emptyForm);
    setDialogMode('create');
    setDialogOpen(true);
  };

  const startEdit = (ledger: Ledger) => {
    setForm({
      id: ledger.id,
      name: ledger.name,
      description: ledger.description || '',
    });
    setDialogMode('edit');
    setDialogOpen(true);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!form.name.trim()) {
      setError('请填写账本名称。');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description.trim(),
      };

      if (form.id) {
        const updated = await updateLedger(form.id, payload);
        setLedgers(current => current.map(ledger => (ledger.id === form.id ? { ...ledger, ...updated, ...payload } : ledger)));
        setSelectedId(form.id);
      } else {
        const created = await createLedger(payload);
        setLedgers(current => [created, ...current]);
        setSelectedId(created.id);
      }
      setForm(emptyForm);
      setDialogOpen(false);
    } catch (err) {
      setError('账本保存失败，请稍后重试。');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (ledger: Ledger) => {
    const confirmed = window.confirm(`确定删除账本“${ledger.name}”吗？`);
    if (!confirmed) {
      return;
    }

    setError(null);
    try {
      await deleteLedger(ledger.id);
      setLedgers(current => current.filter(item => item.id !== ledger.id));
      setSelectedId(current => (current === ledger.id ? undefined : current));
    } catch (err) {
      setError('账本删除失败，请确认你是否有权限。');
    }
  };

  return (
    <div className="everycent-page everycent-ledger-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">账本管理</h1>
          <p className="everycent-page__subtitle">从后端接口读取你的账本，并支持创建、修改和删除。</p>
        </div>
        <button type="button" className="everycent-ledger-page__primary" onClick={startCreate}>
          <FontAwesomeIcon icon="plus" />
          新建账本
        </button>
      </div>

      {error && <div className="everycent-ledger-page__alert">{error}</div>}

      <section className="everycent-ledger-page__grid">
        <article className="everycent-panel everycent-ledger-page__list">
          <header className="everycent-ledger-page__section-header">
            <h2>我的账本</h2>
            <span>{loading ? '加载中' : `${ledgers.length} 个账本`}</span>
          </header>

          {loading ? (
            <div className="everycent-ledger-page__empty">正在读取账本...</div>
          ) : ledgers.length === 0 ? (
            <div className="everycent-ledger-page__empty">还没有账本，先创建一个。</div>
          ) : (
            <div className="everycent-ledger-page__items">
              {ledgers.map(ledger => (
                <button
                  key={ledger.id}
                  type="button"
                  className={`everycent-ledger-page__item${selectedLedger?.id === ledger.id ? ' active' : ''}`}
                  onClick={() => setSelectedId(ledger.id)}
                >
                  <span>
                    <strong>{ledger.name}</strong>
                    <small>{ledger.description || '暂无描述'}</small>
                  </span>
                  <em>{ledger.permissionLevel || ledger.permissionType || '成员'}</em>
                </button>
              ))}
            </div>
          )}
        </article>

        <article className="everycent-panel everycent-ledger-page__detail">
          <header className="everycent-ledger-page__section-header">
            <h2>账本详情</h2>
            {selectedLedger && (
              <div className="everycent-ledger-page__actions">
                <button type="button" onClick={() => startEdit(selectedLedger)}>
                  编辑
                </button>
                <button type="button" className="danger" onClick={() => handleDelete(selectedLedger)}>
                  删除
                </button>
              </div>
            )}
          </header>

          {selectedLedger ? (
            <div className="everycent-ledger-page__detail-grid">
              <div>
                <span>账本名称</span>
                <strong>{selectedLedger.name}</strong>
              </div>
              <div>
                <span>本月结余</span>
                <strong>
                  {selectedLedger.defaultCurrency || 'CNY'} {(selectedLedger.currentMonthBalance || 0).toLocaleString('zh-CN')}
                </strong>
              </div>
              <div>
                <span>创建人</span>
                <strong>{selectedLedger.creatorLogin || '暂无'}</strong>
              </div>
              <div>
                <span>权限</span>
                <strong>{selectedLedger.permissionLevel || selectedLedger.permissionType || '暂无'}</strong>
              </div>
              <div>
                <span>创建日期</span>
                <strong>{formatDate(selectedLedger.createdDate)}</strong>
              </div>
              <div>
                <span>更新日期</span>
                <strong>{formatDate(selectedLedger.lastModifiedDate)}</strong>
              </div>
            </div>
          ) : (
            <div className="everycent-ledger-page__empty">请选择或创建账本。</div>
          )}
        </article>
      </section>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="everycent-ledger-dialog">
          <DialogHeader>
            <DialogTitle>{dialogMode === 'create' ? '新建账本' : '编辑账本'}</DialogTitle>
            <DialogDescription>
              {dialogMode === 'create' ? '创建一个新的 EveryCent 账本，用于记录不同场景下的收支。' : '调整账本名称和描述。'}
            </DialogDescription>
          </DialogHeader>
          <form className="everycent-ledger-page__form" onSubmit={handleSubmit}>
            <label>
              <span>账本名称</span>
              <input value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} />
            </label>
            <label>
              <span>账本描述</span>
              <textarea
                value={form.description}
                onChange={event => setForm(current => ({ ...current, description: event.target.value }))}
              />
            </label>
            <div className="everycent-ledger-page__form-actions">
              <button type="button" onClick={() => setDialogOpen(false)}>
                取消
              </button>
              <button type="submit" className="primary" disabled={saving}>
                {saving ? '保存中...' : '保存账本'}
              </button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default LedgerPage;
