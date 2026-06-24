import './register.scss';

import React, { useEffect, useState } from 'react';
import { ValidatedField, ValidatedForm } from 'react-jhipster';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';

import PasswordStrengthBar from 'app/shared/layout/password/password-strength-bar';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { handleRegister, reset } from './register.reducer';

export const RegisterPage = () => {
  const [password, setPassword] = useState('');
  const dispatch = useAppDispatch();
  const loading = useAppSelector(state => state.register.loading);
  const registrationFailure = useAppSelector(state => state.register.registrationFailure);
  const successMessage = useAppSelector(state => state.register.successMessage);

  useEffect(
    () => () => {
      dispatch(reset());
    },
    [],
  );

  useEffect(() => {
    if (successMessage) {
      toast.success('注册成功，请查看邮箱完成确认。');
    }
  }, [successMessage]);

  const handleValidSubmit = ({ username, firstPassword }) => {
    const normalizedUsername = String(username).trim();
    const internalEmail = `${normalizedUsername.toLowerCase()}@everycent.local`;
    dispatch(handleRegister({ login: normalizedUsername, email: internalEmail, password: firstPassword, langKey: 'zh-cn' }));
  };

  const updatePassword = event => setPassword(event.target.value);

  return (
    <main className="everycent-register-page">
      <section className="everycent-register-shell" aria-labelledby="register-title">
        <div className="everycent-register-hero">
          <div className="everycent-register-brand">
            <img src="/content/images/emotions/calm.png" alt="EveryCent" />
            <span>
              <strong>EveryCent</strong>
              <small>智能账本工作台</small>
            </span>
          </div>

          <div className="everycent-register-hero__copy">
            <span>创建账号</span>
            <h1>开始记录你的每一笔收支</h1>
            <p>注册后可以使用 AI 记账、预算提醒、数据仪表盘和账本协作能力。</p>
          </div>
        </div>

        <section className="everycent-register-card">
          <header>
            <h2 id="register-title" data-cy="registerTitle">
              注册
            </h2>
            <p>填写基础信息，创建你的 EveryCent 账户。</p>
          </header>

          {registrationFailure && <div className="everycent-register-alert">注册失败，请检查账号是否已被使用。</div>}

          <ValidatedForm id="register-form" className="everycent-register-form" onSubmit={handleValidSubmit}>
            <ValidatedField
              name="username"
              label="账号"
              placeholder="请输入账号"
              validate={{
                required: { value: true, message: '账号是必填项。' },
                pattern: {
                  value: /^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$/,
                  message: '账号格式不正确。',
                },
                minLength: { value: 1, message: '账号至少需要 1 个字符。' },
                maxLength: { value: 50, message: '账号不能超过 50 个字符。' },
              }}
              data-cy="username"
            />

            <ValidatedField
              name="firstPassword"
              label="密码"
              placeholder="请输入密码"
              type="password"
              onChange={updatePassword}
              validate={{
                required: { value: true, message: '密码是必填项。' },
                minLength: { value: 4, message: '密码至少需要 4 个字符。' },
                maxLength: { value: 50, message: '密码不能超过 50 个字符。' },
              }}
              data-cy="firstPassword"
            />

            <div className="everycent-register-strength">
              <PasswordStrengthBar password={password} />
            </div>

            <ValidatedField
              name="secondPassword"
              label="确认密码"
              placeholder="请再次输入密码"
              type="password"
              validate={{
                required: { value: true, message: '确认密码是必填项。' },
                minLength: { value: 4, message: '确认密码至少需要 4 个字符。' },
                maxLength: { value: 50, message: '确认密码不能超过 50 个字符。' },
                validate: v => v === password || '两次输入的密码不一致。',
              }}
              data-cy="secondPassword"
            />

            <button id="register-submit" type="submit" data-cy="submit" className="everycent-register-submit" disabled={loading}>
              {loading ? '注册中...' : '创建账号'}
            </button>
          </ValidatedForm>

          <footer>
            <span>已经有账号？</span>
            <Link to="/login">去登录</Link>
          </footer>
        </section>
      </section>
    </main>
  );
};

export default RegisterPage;
