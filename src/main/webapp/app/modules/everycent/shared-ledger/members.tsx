import './members.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { getLedgers, Ledger } from 'app/modules/everycent/ledger/ledger-api';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from 'app/shared/components/everycent/overlays/dialog';

import { deleteLedgerMember, getLedgerMembers, inviteLedgerMember, LedgerMember, updateLedgerMemberPermission } from './member-api';

type DialogMode = 'invite' | 'permission';

type MemberForm = {
  userId: string;
  permissionType: string;
};

const permissionOptions = [
  { value: 'OWNER', label: '所有者' },
  { value: 'READ_WRITE', label: '可读写' },
  { value: 'READ_ONLY', label: '只读' },
];

const emptyForm: MemberForm = {
  userId: '',
  permissionType: 'READ_WRITE',
};

const permissionLabel = (value?: string) => permissionOptions.find(option => option.value === value)?.label || value || '未知权限';

const MembersPage = () => {
  const [ledgers, setLedgers] = useState<Ledger[]>([]);
  const [selectedLedgerId, setSelectedLedgerId] = useState<number>();
  const [members, setMembers] = useState<LedgerMember[]>([]);
  const [selectedMember, setSelectedMember] = useState<LedgerMember | null>(null);
  const [form, setForm] = useState<MemberForm>(emptyForm);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<DialogMode>('invite');
  const [loadingLedgers, setLoadingLedgers] = useState(true);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedLedger = useMemo(() => ledgers.find(ledger => ledger.id === selectedLedgerId), [ledgers, selectedLedgerId]);

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
      setMembers([]);
      return;
    }

    let mounted = true;

    const loadMembers = async () => {
      setLoadingMembers(true);
      setError(null);
      try {
        const data = await getLedgerMembers(selectedLedgerId);
        if (mounted) {
          setMembers(data);
        }
      } catch (err) {
        if (mounted) {
          setError('成员列表加载失败，请确认你是否有该账本权限。');
        }
      } finally {
        if (mounted) {
          setLoadingMembers(false);
        }
      }
    };

    loadMembers();

    return () => {
      mounted = false;
    };
  }, [selectedLedgerId]);

  const openInviteDialog = () => {
    setDialogMode('invite');
    setSelectedMember(null);
    setForm(emptyForm);
    setDialogOpen(true);
  };

  const openPermissionDialog = (member: LedgerMember) => {
    setDialogMode('permission');
    setSelectedMember(member);
    setForm({
      userId: String(member.userId),
      permissionType: member.permissionType,
    });
    setDialogOpen(true);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const ledgerId = selectedLedgerId;
    const userId = Number(form.userId);

    if (!ledgerId) {
      setError('请先选择账本。');
      return;
    }

    if (!userId || Number.isNaN(userId)) {
      setError('请输入有效的用户 ID。');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      if (dialogMode === 'invite') {
        const created = await inviteLedgerMember(ledgerId, {
          userId,
          permissionType: form.permissionType,
        });
        setMembers(current => [
          { ...created, userId, permissionType: form.permissionType },
          ...current.filter(member => member.userId !== userId),
        ]);
      } else {
        await updateLedgerMemberPermission(ledgerId, userId, {
          permissionType: form.permissionType,
        });
        setMembers(current =>
          current.map(member => (member.userId === userId ? { ...member, permissionType: form.permissionType } : member)),
        );
      }

      setDialogOpen(false);
      setForm(emptyForm);
    } catch (err) {
      setError(dialogMode === 'invite' ? '邀请成员失败，请确认用户 ID 和权限。' : '修改成员权限失败。');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (member: LedgerMember) => {
    if (!selectedLedgerId) {
      return;
    }

    const confirmed = window.confirm(`确定移除成员“${member.login || member.userId}”吗？`);
    if (!confirmed) {
      return;
    }

    setError(null);
    try {
      await deleteLedgerMember(selectedLedgerId, member.userId);
      setMembers(current => current.filter(item => item.userId !== member.userId));
    } catch (err) {
      setError('删除成员失败，请确认你是否有管理权限。');
    }
  };

  return (
    <div className="everycent-page everycent-members-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">账本成员</h1>
          <p className="everycent-page__subtitle">选择一个账本，查看成员并管理权限。</p>
        </div>
        <button type="button" className="everycent-members-page__primary" onClick={openInviteDialog} disabled={!selectedLedgerId}>
          <FontAwesomeIcon icon="user-plus" />
          邀请成员
        </button>
      </div>

      {error && <div className="everycent-members-page__alert">{error}</div>}

      <section className="everycent-panel everycent-members-page__toolbar">
        <label>
          <span>当前账本</span>
          <select
            value={selectedLedgerId || ''}
            onChange={event => setSelectedLedgerId(event.target.value ? Number(event.target.value) : undefined)}
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
        <div>
          <span>成员数量</span>
          <strong>{loadingMembers ? '加载中' : `${members.length} 人`}</strong>
        </div>
        <div>
          <span>账本描述</span>
          <strong>{selectedLedger?.description || '暂无描述'}</strong>
        </div>
      </section>

      <section className="everycent-panel everycent-members-page__table-panel">
        <header className="everycent-members-page__section-header">
          <h2>成员列表</h2>
          <span>{selectedLedger?.name || '未选择账本'}</span>
        </header>

        {loadingMembers ? (
          <div className="everycent-members-page__empty">正在读取成员...</div>
        ) : members.length === 0 ? (
          <div className="everycent-members-page__empty">当前账本还没有可展示的成员。</div>
        ) : (
          <div className="everycent-members-page__table-wrap">
            <table className="everycent-members-page__table">
              <thead>
                <tr>
                  <th>用户</th>
                  <th>邮箱</th>
                  <th>权限</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                {members.map(member => (
                  <tr key={member.userId}>
                    <td>
                      <div className="everycent-members-page__user">
                        <span>{(member.login || String(member.userId)).slice(0, 2).toUpperCase()}</span>
                        <strong>{member.login || `用户 ${member.userId}`}</strong>
                      </div>
                    </td>
                    <td>{member.email || '暂无邮箱'}</td>
                    <td>
                      <em>{permissionLabel(member.permissionType)}</em>
                    </td>
                    <td>
                      <div className="everycent-members-page__actions">
                        <button type="button" onClick={() => openPermissionDialog(member)}>
                          改权限
                        </button>
                        <button type="button" className="danger" onClick={() => handleDelete(member)}>
                          移除
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="everycent-member-dialog">
          <DialogHeader>
            <DialogTitle>{dialogMode === 'invite' ? '邀请账本成员' : '修改成员权限'}</DialogTitle>
            <DialogDescription>
              {dialogMode === 'invite' ? '输入用户 ID 并选择该成员在当前账本中的权限。' : '只会修改该成员在当前账本中的权限。'}
            </DialogDescription>
          </DialogHeader>

          <form className="everycent-members-page__form" onSubmit={handleSubmit}>
            <label>
              <span>用户 ID</span>
              <input
                type="number"
                min="1"
                value={form.userId}
                disabled={dialogMode === 'permission'}
                onChange={event => setForm(current => ({ ...current, userId: event.target.value }))}
              />
            </label>
            <label>
              <span>权限</span>
              <select
                value={form.permissionType}
                onChange={event => setForm(current => ({ ...current, permissionType: event.target.value }))}
              >
                {permissionOptions.map(option => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            {selectedMember && (
              <div className="everycent-members-page__dialog-note">
                当前成员：<strong>{selectedMember.login}</strong>
              </div>
            )}
            <div className="everycent-members-page__form-actions">
              <button type="button" onClick={() => setDialogOpen(false)}>
                取消
              </button>
              <button type="submit" className="primary" disabled={saving}>
                {saving ? '保存中...' : dialogMode === 'invite' ? '发送邀请' : '保存权限'}
              </button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default MembersPage;
