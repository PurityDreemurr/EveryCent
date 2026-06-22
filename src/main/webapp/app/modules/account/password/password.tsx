import '../settings/account-pages.scss';

import React, { useEffect, useState } from 'react';
import { ValidatedField, ValidatedForm } from 'react-jhipster';
import { toast } from 'react-toastify';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSession } from 'app/shared/reducers/authentication';
import PasswordStrengthBar from 'app/shared/layout/password/password-strength-bar';
import { reset, savePassword } from './password.reducer';

export const PasswordPage = () => {
  const [password, setPassword] = useState('');
  const dispatch = useAppDispatch();
  const account = useAppSelector(state => state.authentication.account);
  const successMessage = useAppSelector(state => state.password.successMessage);
  const errorMessage = useAppSelector(state => state.password.errorMessage);
  const loading = useAppSelector(state => state.password.loading);

  useEffect(() => {
    dispatch(reset());
    dispatch(getSession());
    return () => {
      dispatch(reset());
    };
  }, []);

  useEffect(() => {
    if (successMessage) {
      toast.success('密码已更新');
    } else if (errorMessage) {
      toast.error('密码修改失败，请检查当前密码后重试。');
    }
    dispatch(reset());
  }, [successMessage, errorMessage]);

  const handleValidSubmit = ({ currentPassword, newPassword }) => {
    dispatch(savePassword({ currentPassword, newPassword }));
  };

  const updatePassword = event => setPassword(event.target.value);

  return (
    <div className="everycent-page everycent-account-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">修改密码</h1>
          <p className="everycent-page__subtitle">为账号 {account.login || '当前用户'} 更新登录密码，建议使用不易猜测的组合。</p>
        </div>
      </div>

      <section className="everycent-panel everycent-account-page__panel everycent-account-page__panel--narrow">
        <ValidatedForm id="password-form" className="everycent-account-page__form" onSubmit={handleValidSubmit}>
          <ValidatedField
            name="currentPassword"
            label="当前密码"
            placeholder="请输入当前密码"
            type="password"
            validate={{
              required: { value: true, message: '当前密码是必填项。' },
            }}
            data-cy="currentPassword"
          />

          <ValidatedField
            name="newPassword"
            label="新密码"
            placeholder="请输入新密码"
            type="password"
            validate={{
              required: { value: true, message: '新密码是必填项。' },
              minLength: { value: 4, message: '新密码至少需要 4 个字符。' },
              maxLength: { value: 50, message: '新密码不能超过 50 个字符。' },
            }}
            onChange={updatePassword}
            data-cy="newPassword"
          />

          <div className="everycent-account-page__strength">
            <PasswordStrengthBar password={password} />
          </div>

          <ValidatedField
            name="confirmPassword"
            label="确认新密码"
            placeholder="请再次输入新密码"
            type="password"
            validate={{
              required: { value: true, message: '确认密码是必填项。' },
              minLength: { value: 4, message: '确认密码至少需要 4 个字符。' },
              maxLength: { value: 50, message: '确认密码不能超过 50 个字符。' },
              validate: v => v === password || '两次输入的密码不一致。',
            }}
            data-cy="confirmPassword"
          />

          <div className="everycent-account-page__actions">
            <button type="submit" data-cy="submit" disabled={loading}>
              <FontAwesomeIcon icon="lock" />
              {loading ? '更新中...' : '更新密码'}
            </button>
          </div>
        </ValidatedForm>
      </section>
    </div>
  );
};

export default PasswordPage;
