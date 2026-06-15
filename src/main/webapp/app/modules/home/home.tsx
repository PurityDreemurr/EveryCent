import './home.scss';

import React from 'react';
import { Link } from 'react-router-dom';

import { Alert, Col, Row } from 'reactstrap';

import { useAppSelector } from 'app/config/store';

/* ========== 内联样式 ========== */
const s = {
  page: {
    maxWidth: 960,
    margin: '0 auto',
    padding: '2rem 1rem',
  } as React.CSSProperties,
  hero: {
    textAlign: 'center',
    padding: '3rem 1rem 2rem',
  } as React.CSSProperties,
  logo: {
    width: 100,
    height: 100,
    borderRadius: 28,
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 1.5rem',
    boxShadow: '0 8px 30px rgba(102,126,234,0.3)',
    fontSize: '2.2rem',
    fontWeight: 800,
    color: '#fff',
  } as React.CSSProperties,
  title: {
    fontSize: '2.2rem',
    fontWeight: 800,
    color: '#2d2d4a',
    marginBottom: '0.4rem',
  } as React.CSSProperties,
  highlight: {
    background: 'linear-gradient(135deg, #667eea, #764ba2)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  } as React.CSSProperties,
  subtitle: {
    fontSize: '1.05rem',
    color: '#8e8ea8',
    fontWeight: 400,
    marginBottom: 0,
  } as React.CSSProperties,
  card: {
    background: '#fff',
    borderRadius: 14,
    boxShadow: '0 2px 20px rgba(0,0,0,0.06)',
    padding: '1.8rem 2rem',
    marginBottom: '1.5rem',
  } as React.CSSProperties,
  cardTitle: {
    fontSize: '1.05rem',
    fontWeight: 700,
    color: '#4a4a6a',
    marginBottom: '1rem',
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  } as React.CSSProperties,
  alert: {
    border: 'none',
    borderRadius: 10,
    padding: '0.85rem 1.1rem',
    fontSize: '0.9rem',
  } as React.CSSProperties,
  alertSuccess: {
    ...({} as React.CSSProperties),
    border: 'none',
    borderRadius: 10,
    padding: '0.85rem 1.1rem',
    fontSize: '0.9rem',
    background: '#eafaf1',
    color: '#1e7e34',
    borderLeft: '4px solid #28a745',
  } as React.CSSProperties,
  alertWarning: {
    border: 'none',
    borderRadius: 10,
    padding: '0.85rem 1.1rem',
    fontSize: '0.9rem',
    background: '#fff9e6',
    color: '#8a6d14',
    borderLeft: '4px solid #ffc107',
  } as React.CSSProperties,
  loginLink: {
    color: '#667eea',
    fontWeight: 700,
    textDecoration: 'none',
  } as React.CSSProperties,
  accountList: {
    margin: '0.35rem 0 0',
    paddingLeft: '1.2rem',
    fontSize: '0.88rem',
    color: '#6e6e8a',
    lineHeight: 1.8,
  } as React.CSSProperties,
  sectionTitle: {
    fontSize: '1rem',
    fontWeight: 700,
    color: '#4a4a6a',
    marginBottom: '0.8rem',
  } as React.CSSProperties,
  resourceGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))',
    gap: '0.75rem',
  } as React.CSSProperties,
  resourceItem: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '0.7rem 1rem',
    borderRadius: 10,
    background: '#f8f9fc',
    textDecoration: 'none',
    fontSize: '0.88rem',
    fontWeight: 500,
    color: '#5a5a7a',
    transition: 'all 0.2s',
  } as React.CSSProperties,
  resourceIcon: {
    width: 34,
    height: 34,
    borderRadius: 8,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1rem',
    flexShrink: 0,
  } as React.CSSProperties,
  footerNote: {
    textAlign: 'center',
    marginTop: '1rem',
    fontSize: '0.85rem',
    color: '#b0b0c0',
  } as React.CSSProperties,
  footerLink: {
    color: '#667eea',
    fontWeight: 600,
    textDecoration: 'none',
  } as React.CSSProperties,
};

const iconColors = ['#667eea', '#764ba2', '#f39c12', '#2ecc71', '#e74c3c', '#3498db'];

export const Home = () => {
  const account = useAppSelector(state => state.authentication.account);

  const resources = [
    { label: 'EveryCent 文档', href: '#', emoji: '📖' },
    { label: 'Stack Overflow', href: 'https://stackoverflow.com/', emoji: '💬' },
    { label: '问题反馈', href: '#', emoji: '🐛' },
    { label: '交流社区', href: '#', emoji: '👥' },
    { label: 'Twitter', href: 'https://twitter.com/', emoji: '🐦' },
    { label: 'GitHub', href: '#', emoji: '⭐' },
  ];

  return (
    <div style={s.page}>
      {/* ===== Hero ===== */}
      <div style={s.hero}>
        <div style={s.logo}>EC</div>
        <h1 style={s.title}>
          欢迎使用 <span style={s.highlight}>EveryCent</span>
        </h1>
        <p style={s.subtitle}>智慧记账，让每一分钱都有迹可循</p>
      </div>

      {/* ===== 账号状态 ===== */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span>🔐</span> 账号状态
        </div>
        {account?.login ? (
          <Alert color="success" style={s.alertSuccess}>
            ✅ 您目前是以 <strong>&quot;{account.login}&quot;</strong> 账号登录，欢迎回来！
          </Alert>
        ) : (
          <div>
            <Alert color="warning" style={s.alertWarning}>
              💡 如果您要
              <Link to="/login" style={s.loginLink}>
                &nbsp;登录
              </Link>
              ，可以使用默认账号：
              <ul style={s.accountList}>
                <li>
                  管理员（账号=<strong>admin</strong>，密码=<strong>admin</strong>）
                </li>
                <li>
                  普通用户（账号=<strong>user</strong>，密码=<strong>user</strong>）
                </li>
              </ul>
            </Alert>
            <Alert color="warning" style={s.alertWarning}>
              🆕 还没有账号？&nbsp;
              <Link to="/account/register" style={s.loginLink}>
                注册一个新账号
              </Link>
            </Alert>
          </div>
        )}
      </div>

      {/* ===== 快捷入口 ===== */}
      <div style={s.card}>
        <div style={s.sectionTitle}>🚀 快速开始</div>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' } as React.CSSProperties}>
          {!account?.login && (
            <Link
              to="/login"
              style={{
                ...s.resourceItem,
                background: 'linear-gradient(135deg, #667eea, #764ba2)',
                color: '#fff',
                fontWeight: 600,
              }}
            >
              <span>🔑</span> 立即登录
            </Link>
          )}
          <Link to="/account/register" style={s.resourceItem}>
            <span>📝</span> 注册账号
          </Link>
        </div>
      </div>

      {/* ===== 资源链接 ===== */}
      <div style={s.card}>
        <div style={s.sectionTitle}>📚 相关资源</div>
        <div style={s.resourceGrid}>
          {resources.map((res, i) => (
            <a
              key={res.label}
              href={res.href}
              target="_blank"
              rel="noopener noreferrer"
              style={s.resourceItem}
              onMouseEnter={e => {
                (e.currentTarget as HTMLAnchorElement).style.background = '#eeeef8';
              }}
              onMouseLeave={e => {
                (e.currentTarget as HTMLAnchorElement).style.background = '#f8f9fc';
              }}
            >
              <span
                style={{
                  ...s.resourceIcon,
                  background: `${iconColors[i % iconColors.length]}15`,
                  color: iconColors[i % iconColors.length],
                }}
              >
                {res.emoji}
              </span>
              {res.label}
            </a>
          ))}
        </div>
      </div>

      <p style={s.footerNote}>
        ❤️ 如果您喜欢 EveryCent，请在{' '}
        <a href="#" style={s.footerLink}>
          GitHub
        </a>{' '}
        上给我们一颗星！
      </p>
    </div>
  );
};

export default Home;
