import './export.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { getLedgers, Ledger } from 'app/modules/everycent/ledger/ledger-api';

import { exportLedgerTransactions } from './export-api';

type ExportForm = {
  ledgerId: string;
  startDate: string;
  endDate: string;
};

const isoDate = (date: Date) => date.toISOString().slice(0, 10);

const defaultForm = (): ExportForm => {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 30);

  return {
    ledgerId: '',
    startDate: isoDate(start),
    endDate: isoDate(end),
  };
};

const downloadBlob = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

const ExportPage = () => {
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [form, setForm] = useState<ExportForm>(defaultForm);
  const [loadingLedgers, setLoadingLedgers] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const selectedLedger = useMemo(() => ledgers.find(ledger => String(ledger.id) === form.ledgerId), [form.ledgerId, ledgers]);

  useEffect(() => {
    let mounted = true;

    const loadLedgers = async () => {
      setLoadingLedgers(true);
      setError(null);
      try {
        const data = await getLedgers();
        if (mounted) {
          setLedgers(data);
          setForm(current => ({ ...current, ledgerId: current.ledgerId || String(data[0]?.id || '') }));
        }
      } catch (err) {
        if (mounted) {
          setError('账本列表加载失败，请检查登录状态和后端接口。');
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

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!form.ledgerId) {
      setError('请先选择要导出的账本。');
      return;
    }

    if (!form.startDate || !form.endDate) {
      setError('请选择导出开始日期和结束日期。');
      return;
    }

    if (form.startDate > form.endDate) {
      setError('开始日期不能晚于结束日期。');
      return;
    }

    setExporting(true);
    setError(null);
    setSuccess(null);
    try {
      const result = await exportLedgerTransactions(Number(form.ledgerId), {
        startDate: form.startDate,
        endDate: form.endDate,
      });
      downloadBlob(result.blob, result.filename);
      setSuccess(`已生成 ${selectedLedger?.name || '账本'} 的 Excel 文件。`);
    } catch (err) {
      setError('导出失败，请检查日期范围、账本权限和后端导出接口。');
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="everycent-page everycent-export-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">导出 Excel</h1>
          <p className="everycent-page__subtitle">按账本和日期范围导出收支记录，生成可下载的 Excel 文件。</p>
        </div>
      </div>

      {error && <div className="everycent-export-page__alert">{error}</div>}
      {success && <div className="everycent-export-page__success">{success}</div>}

      <section className="everycent-panel everycent-export-page__panel">
        <form className="everycent-export-page__form" onSubmit={handleSubmit}>
          <label>
            <span>账本</span>
            <select
              value={form.ledgerId}
              onChange={event => setForm(current => ({ ...current, ledgerId: event.target.value }))}
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
            <span>开始日期</span>
            <input
              type="date"
              value={form.startDate}
              onChange={event => setForm(current => ({ ...current, startDate: event.target.value }))}
            />
          </label>

          <label>
            <span>结束日期</span>
            <input type="date" value={form.endDate} onChange={event => setForm(current => ({ ...current, endDate: event.target.value }))} />
          </label>

          <button type="submit" className="everycent-export-page__submit" disabled={exporting || loadingLedgers || !form.ledgerId}>
            <FontAwesomeIcon icon="save" />
            {exporting ? '导出中...' : '导出文件'}
          </button>
        </form>
      </section>

      <section className="everycent-export-page__notes">
        <div>
          <strong>{selectedLedger?.name || '未选择账本'}</strong>
          <span>
            {form.startDate || '开始日期'} 至 {form.endDate || '结束日期'}
          </span>
        </div>
        <div>
          <strong>Excel 文件</strong>
          <span>包含该账本指定日期范围内的收支记录。</span>
        </div>
      </section>
    </div>
  );
};

export default ExportPage;
