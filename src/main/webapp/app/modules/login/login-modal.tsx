import './login-modal.scss';

import React, { useEffect, useRef } from 'react';
import { gsap } from 'gsap';
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
  const pageRef = useRef<HTMLElement>(null);

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

  useEffect(() => {
    if (!props.showModal || !pageRef.current) {
      return undefined;
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const ctx = gsap.context(() => {
      if (reduceMotion) {
        gsap.set('.everycent-login-shell, .everycent-login-brand, .everycent-login-hero__copy > *, .everycent-login-card > *', {
          autoAlpha: 1,
          clearProps: 'transform',
        });
        return;
      }

      const intro = gsap.timeline({
        defaults: {
          duration: 0.58,
          ease: 'power3.out',
        },
      });

      intro
        .from('.everycent-login-shell', { autoAlpha: 0, y: 18, scale: 0.985 })
        .from('.everycent-login-brand', { autoAlpha: 0, y: -12 }, '<0.12')
        .from('.everycent-login-hero__copy > *', { autoAlpha: 0, y: 18, stagger: 0.08 }, '<0.08')
        .from('.everycent-login-card > *', { autoAlpha: 0, y: 14, stagger: 0.07 }, '<0.12');
    }, pageRef);

    return () => ctx.revert();
  }, [props.showModal]);

  useEffect(() => {
    if (!props.loginError || !pageRef.current) {
      return undefined;
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const ctx = gsap.context(() => {
      gsap.fromTo(
        '.everycent-login-alert',
        { autoAlpha: 0, y: reduceMotion ? 0 : -6 },
        { autoAlpha: 1, y: 0, duration: reduceMotion ? 0 : 0.28, ease: 'power2.out' },
      );
    }, pageRef);

    return () => ctx.revert();
  }, [props.loginError]);

  if (!props.showModal) {
    return null;
  }

  return (
    <main className="everycent-login-page" id="login-page" ref={pageRef}>
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
