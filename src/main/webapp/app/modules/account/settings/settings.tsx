import './account-pages.scss';

import axios from 'axios';
import React, { useEffect, useState } from 'react';
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
  const [qqBinding, setQqBinding] = useState<{ bindingCode?: string; bound?: boolean; qqOpenIdMask?: string } | null>(null);
  const [qqBindingResetting, setQqBindingResetting] = useState(false);

  useEffect(() => {
    dispatch(getSession());
    axios.get('api/account/qq-bot-binding').then(response => setQqBinding(response.data));
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

  const qqBindText = qqBinding?.bindingCode ? `绑定 ${qqBinding.bindingCode}` : '';

  const copyQqBindText = async () => {
    if (!qqBindText) {
      return;
    }
    const copied = await copyText(qqBindText);
    if (copied) {
      toast.success('机器人绑定指令已复制');
    } else {
      toast.error('复制失败，请手动选中绑定指令复制');
    }
  };

  const copyText = async (text: string) => {
    try {
      if (navigator.clipboard?.writeText && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
        return true;
      }
    } catch (error) {
      // Fall through to the textarea fallback for browsers that block clipboard access.
    }
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', 'true');
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    textarea.style.top = '0';
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
      return document.execCommand('copy');
    } finally {
      document.body.removeChild(textarea);
    }
  };

  const resetQqBinding = async () => {
    if (!qqBinding?.bound || qqBindingResetting) {
      return;
    }
    if (!window.confirm('确定要解绑当前 QQ 并生成新的绑定指令吗？旧绑定会立即失效。')) {
      return;
    }
    setQqBindingResetting(true);
    try {
      const response = await axios.delete('api/account/qq-bot-binding');
      setQqBinding(response.data);
      toast.success('QQ 机器人已解绑，新的绑定指令已生成');
    } finally {
      setQqBindingResetting(false);
    }
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

      <section className="everycent-panel everycent-account-page__panel everycent-account-page__bot">
        <div className="everycent-account-page__bot-main">
          <img className="everycent-account-page__bot-qr" src="/content/images/qq-bot-qr.png" alt="QQ 机器人二维码" />
          <div>
            <h2>QQ 机器人绑定</h2>
            <p>使用 QQ 扫码添加机器人，再复制绑定指令发送给机器人，之后机器人会使用当前 EveryCent 账户处理记账、查账和导出。</p>
          </div>
        </div>
        <div className="everycent-account-page__bot-code">
          <span>{qqBindText || '加载中...'}</span>
          <button type="button" onClick={copyQqBindText} disabled={!qqBindText}>
            <FontAwesomeIcon icon="copy" />
            复制
          </button>
          {qqBinding?.bound && (
            <button className="everycent-account-page__bot-reset" type="button" onClick={resetQqBinding} disabled={qqBindingResetting}>
              <FontAwesomeIcon icon="times-circle" />
              {qqBindingResetting ? '解绑中...' : '解绑'}
            </button>
          )}
        </div>
        <small>
          {qqBinding?.bound ? `已绑定 QQ：${qqBinding.qqOpenIdMask || '已绑定'}` : '当前账户尚未绑定 QQ。一个账户只能绑定一个 QQ。'}
        </small>
      </section>
    </div>
  );
};

export default SettingsPage;
