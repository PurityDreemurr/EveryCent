import './login-modal.scss';

import React from 'react';
import { type FieldError, useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Alert, Button, Form } from 'reactstrap';
import { ValidatedField } from 'react-jhipster';

export interface ILoginModalProps {
  showModal: boolean;
  loginError: boolean;
  handleLogin: (username: string, password: string, rememberMe: boolean) => void;
  handleClose: () => void;
}

const LoginModal = (props: ILoginModalProps) => {
  const {
    handleSubmit,
    register,
    formState: { errors, touchedFields },
  } = useForm({ mode: 'onTouched' });

  const login = ({ username, password, rememberMe }) => {
    props.handleLogin(username, password, rememberMe);
  };

  const handleLoginSubmit = event => {
    handleSubmit(login)(event);
  };

  if (!props.showModal) {
    return null;
  }

  return (
    <main className="everycent-login-page" id="login-page">
      <section className="everycent-login-shell" aria-labelledby="login-title">
        <div className="everycent-login-hero">
          <div className="everycent-login-brand">
            <span className="everycent-login-brand__mark">EC</span>
            <span>
              <strong>EveryCent</strong>
              <small>智能账本工作台</small>
            </span>
          </div>

          <div className="everycent-login-hero__copy">
            <span className="everycent-login-hero__eyebrow">欢迎回来</span>
            <h1>回到你的财务工作区</h1>
            <p>登录后继续使用 AI 记账、预算提醒和数据仪表盘。</p>
          </div>
        </div>

        <section className="everycent-login-card">
          <header>
            <h2 id="login-title" data-cy="loginTitle">
              登录
            </h2>
            <p>使用你的 EveryCent 账户继续。</p>
          </header>

          {props.loginError && (
            <Alert color="danger" data-cy="loginError" className="everycent-login-alert">
              <strong>登录失败。</strong> 请检查账号和密码后重试。
            </Alert>
          )}

          <Form onSubmit={handleLoginSubmit} className="everycent-login-form">
            <ValidatedField
              name="username"
              label="账号"
              placeholder="请输入账号"
              required
              autoFocus
              data-cy="username"
              validate={{ required: '请输入账号。' }}
              register={register}
              error={errors.username as FieldError}
              isTouched={touchedFields.username}
            />
            <ValidatedField
              name="password"
              type="password"
              label="密码"
              placeholder="请输入密码"
              required
              data-cy="password"
              validate={{ required: '请输入密码。' }}
              register={register}
              error={errors.password as FieldError}
              isTouched={touchedFields.password}
            />

            <div className="everycent-login-options">
              <ValidatedField name="rememberMe" type="checkbox" check label="记住我" value={true} register={register} />
              <Link to="/account/reset/request" data-cy="forgetYourPasswordSelector">
                忘记密码？
              </Link>
            </div>

            <Button color="primary" type="submit" data-cy="submit" className="everycent-login-submit">
              登录
            </Button>
          </Form>

          <div className="everycent-login-card__footer">
            <span>还没有账号？</span>
            <Link to="/account/register">注册新账号</Link>
          </div>
        </section>
      </section>
    </main>
  );
};

export default LoginModal;
