import './account-pages.scss';

import React, { useEffect } from 'react';
import { ValidatedField, ValidatedForm, isEmail } from 'react-jhipster';
import { toast } from 'react-toastify';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSession } from 'app/shared/reducers/authentication';
import { reset, saveAccountSettings } from './settings.reducer';

export const SettingsPage = () => {
  const dispatch = useAppDispatch();
  const account = useAppSelector(state => state.authentication.account);
  const successMessage = useAppSelector(state => state.settings.successMessage);
  const loading = useAppSelector(state => state.settings.loading);

  useEffect(() => {
    dispatch(getSession());
    return () => {
      dispatch(reset());
    };
  }, []);

  useEffect(() => {
    if (successMessage) {
      toast.success('个人资料已更新');
    }
  }, [successMessage]);

  const handleValidSubmit = values => {
    dispatch(
      saveAccountSettings({
        ...account,
        ...values,
      }),
    );
  };

  return (
    <div className="everycent-page everycent-account-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">个人资料</h1>
          <p className="everycent-page__subtitle">维护你的账号名称和联系邮箱，这些信息会用于账户识别和通知。</p>
        </div>
      </div>

      <section className="everycent-panel everycent-account-page__panel">
        <div className="everycent-account-page__identity">
          <span>{(account.login || 'EC').slice(0, 2).toUpperCase()}</span>
          <div>
            <strong>{account.login}</strong>
            <small>{account.email || '尚未设置邮箱'}</small>
          </div>
        </div>

        <ValidatedForm id="settings-form" className="everycent-account-page__form" onSubmit={handleValidSubmit} defaultValues={account}>
          <div className="everycent-account-page__grid">
            <ValidatedField
              name="firstName"
              label="名字"
              id="firstName"
              placeholder="请输入名字"
              validate={{
                required: { value: true, message: '名字是必填项。' },
                minLength: { value: 1, message: '名字至少需要 1 个字符。' },
                maxLength: { value: 50, message: '名字不能超过 50 个字符。' },
              }}
              data-cy="firstname"
            />
            <ValidatedField
              name="lastName"
              label="姓氏"
              id="lastName"
              placeholder="请输入姓氏"
              validate={{
                required: { value: true, message: '姓氏是必填项。' },
                minLength: { value: 1, message: '姓氏至少需要 1 个字符。' },
                maxLength: { value: 50, message: '姓氏不能超过 50 个字符。' },
              }}
              data-cy="lastname"
            />
          </div>

          <ValidatedField
            name="email"
            label="电子邮箱"
            placeholder="请输入电子邮箱"
            type="email"
            validate={{
              required: { value: true, message: '电子邮箱是必填项。' },
              minLength: { value: 5, message: '电子邮箱至少需要 5 个字符。' },
              maxLength: { value: 254, message: '电子邮箱不能超过 254 个字符。' },
              validate: v => isEmail(v) || '电子邮箱格式不正确。',
            }}
            data-cy="email"
          />

          <div className="everycent-account-page__actions">
            <button type="submit" data-cy="submit" disabled={loading}>
              <FontAwesomeIcon icon="save" />
              {loading ? '保存中...' : '保存资料'}
            </button>
          </div>
        </ValidatedForm>
      </section>
    </div>
  );
};

export default SettingsPage;
