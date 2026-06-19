import './notifications.scss';

import React, { useEffect, useMemo, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  deleteNotification,
  getNotifications,
  markNotificationAsRead,
  NotificationMessage,
  NotificationReadFilter,
} from './notification-api';

const pageSize = 10;

const filterOptions: { value: NotificationReadFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
  { value: 'read', label: '已读' },
];

const levelLabel = (level?: string) => {
  if (level === 'DANGER') {
    return '严重';
  }
  if (level === 'WARNING') {
    return '预警';
  }
  if (level === 'INFO') {
    return '提醒';
  }
  return level || '通知';
};

const typeLabel = (type?: string) => {
  if (type === 'BUDGET_ALERT') {
    return '预算';
  }
  if (type === 'AI_ALERT') {
    return 'AI';
  }
  if (type === 'SYSTEM') {
    return '系统';
  }
  return type || '消息';
};

const formatDateTime = (value?: string) => {
  if (!value) {
    return '未知时间';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

const readParamFromFilter = (filter: NotificationReadFilter) => {
  if (filter === 'read') {
    return true;
  }
  if (filter === 'unread') {
    return false;
  }
  return undefined;
};

const NotificationsPage = () => {
  const [notifications, setNotifications] = useState<NotificationMessage[]>([]);
  const [filter, setFilter] = useState<NotificationReadFilter>('all');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  const totalPages = useMemo(() => Math.max(1, Math.ceil(totalElements / pageSize)), [totalElements]);
  const unreadCount = useMemo(() => notifications.filter(notification => !notification.read).length, [notifications]);

  const loadNotifications = async (nextPage = page, nextFilter = filter) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getNotifications({
        read: readParamFromFilter(nextFilter),
        page: nextPage,
        size: pageSize,
      });
      setNotifications(data.content || []);
      setTotalElements(data.totalElements || 0);
      setPage(data.page || nextPage);
    } catch (err) {
      setError('通知加载失败，请检查后端接口和登录状态。');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications(0, filter);
  }, [filter]);

  const handleFilterChange = (nextFilter: NotificationReadFilter) => {
    setPage(0);
    setFilter(nextFilter);
  };

  const handleMarkAsRead = async (notification: NotificationMessage) => {
    if (notification.read) {
      return;
    }

    setBusyId(notification.id);
    setError(null);
    try {
      const updated = await markNotificationAsRead(notification.id);
      setNotifications(current => current.map(item => (item.id === notification.id ? updated : item)));
    } catch (err) {
      setError('标记已读失败，请稍后再试。');
    } finally {
      setBusyId(null);
    }
  };

  const handleDelete = async (notification: NotificationMessage) => {
    const confirmed = window.confirm(`确定删除通知“${notification.title}”吗？`);
    if (!confirmed) {
      return;
    }

    setBusyId(notification.id);
    setError(null);
    try {
      await deleteNotification(notification.id);
      await loadNotifications(page, filter);
    } catch (err) {
      setError('删除通知失败，请稍后再试。');
    } finally {
      setBusyId(null);
    }
  };

  const goToPage = (nextPage: number) => {
    if (nextPage < 0 || nextPage >= totalPages || nextPage === page) {
      return;
    }
    loadNotifications(nextPage, filter);
  };

  return (
    <div className="everycent-page everycent-notifications-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">消息提醒</h1>
          <p className="everycent-page__subtitle">查看预算预警、AI 提醒和系统通知，并及时处理未读消息。</p>
        </div>
        <button type="button" className="everycent-notifications-page__refresh" onClick={() => loadNotifications(page, filter)}>
          <FontAwesomeIcon icon="sync" />
          刷新
        </button>
      </div>

      {error && <div className="everycent-notifications-page__alert">{error}</div>}

      <section className="everycent-panel everycent-notifications-page__toolbar">
        <div className="everycent-notifications-page__filters" role="group" aria-label="通知筛选">
          {filterOptions.map(option => (
            <button
              key={option.value}
              type="button"
              className={filter === option.value ? 'active' : ''}
              onClick={() => handleFilterChange(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
        <div className="everycent-notifications-page__summary">
          <span>当前页未读</span>
          <strong>{unreadCount}</strong>
        </div>
      </section>

      <section className="everycent-panel everycent-notifications-page__list">
        {loading ? (
          <div className="everycent-notifications-page__empty">正在读取通知...</div>
        ) : notifications.length === 0 ? (
          <div className="everycent-notifications-page__empty">当前没有通知</div>
        ) : (
          <div className="everycent-notifications-page__items">
            {notifications.map(notification => (
              <article key={notification.id} className={`everycent-notifications-page__item${notification.read ? '' : ' unread'}`}>
                <span className={`everycent-notifications-page__level level-${(notification.level || 'INFO').toLowerCase()}`}>
                  {levelLabel(notification.level)}
                </span>
                <div className="everycent-notifications-page__content">
                  <div className="everycent-notifications-page__title-row">
                    <h2>{notification.title}</h2>
                    {!notification.read && <span className="everycent-notifications-page__dot">未读</span>}
                  </div>
                  <p>{notification.content}</p>
                  <div className="everycent-notifications-page__meta">
                    <span>{typeLabel(notification.type)}</span>
                    <span>{formatDateTime(notification.createdDate)}</span>
                    {notification.ledgerId && <span>账本 #{notification.ledgerId}</span>}
                    {notification.budgetId && <span>预算 #{notification.budgetId}</span>}
                  </div>
                </div>
                <div className="everycent-notifications-page__actions">
                  <button
                    type="button"
                    onClick={() => handleMarkAsRead(notification)}
                    disabled={notification.read || busyId === notification.id}
                  >
                    已读
                  </button>
                  <button type="button" className="danger" onClick={() => handleDelete(notification)} disabled={busyId === notification.id}>
                    <FontAwesomeIcon icon="trash" />
                  </button>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>

      <nav className="everycent-notifications-page__pagination" aria-label="通知分页">
        <button type="button" onClick={() => goToPage(page - 1)} disabled={loading || page <= 0}>
          上一页
        </button>
        <span>
          第 {page + 1} / {totalPages} 页，共 {totalElements} 条
        </span>
        <button type="button" onClick={() => goToPage(page + 1)} disabled={loading || page + 1 >= totalPages}>
          下一页
        </button>
      </nav>
    </div>
  );
};

export default NotificationsPage;
