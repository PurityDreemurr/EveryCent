import React from 'react';
import { ValidatedField } from 'react-jhipster';
import { Alert, Button, Col, Form, Modal, ModalBody, ModalFooter, ModalHeader, Row } from 'reactstrap';
import { Link } from 'react-router-dom';
import { type FieldError, useForm } from 'react-hook-form';

export interface ILoginModalProps {
  showModal: boolean;
  loginError: boolean;
  handleLogin: (username: string, password: string, rememberMe: boolean) => void;
  handleClose: () => void;
}

/* ========== 内联样式 ========== */
const styles = {
  modalContent: {
    border: 'none',
    borderRadius: 12,
    boxShadow: '0 8px 40px rgba(0,0,0,0.12)',
    overflow: 'hidden',
  } as React.CSSProperties,
  modalHeader: {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    borderBottom: 'none',
    padding: '1.75rem 1.5rem 1.5rem',
  } as React.CSSProperties,
  brandArea: {
    display: 'flex',
    alignItems: 'center',
    gap: 14,
  } as React.CSSProperties,
  brandIcon: {
    width: 48,
    height: 48,
    borderRadius: 12,
    background: 'rgba(255,255,255,0.2)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1.15rem',
    fontWeight: 700,
    color: '#fff',
    letterSpacing: 1,
    backdropFilter: 'blur(4px)',
  } as React.CSSProperties,
  brandText: {
    fontSize: '1.25rem',
    fontWeight: 700,
    color: '#fff',
    display: 'block',
    lineHeight: 1.3,
  } as React.CSSProperties,
  brandDesc: {
    margin: 0,
    fontSize: '0.85rem',
    color: 'rgba(255,255,255,0.75)',
    fontWeight: 400,
    lineHeight: 1.4,
  } as React.CSSProperties,
  closeBtn: {
    filter: 'brightness(0) invert(1)',
    opacity: 0.8,
  } as React.CSSProperties,
  modalBody: {
    padding: '1.75rem 1.5rem 1rem',
  } as React.CSSProperties,
  formGroup: {
    marginBottom: '1.15rem',
  } as React.CSSProperties,
  formLabel: {
    fontSize: '0.85rem',
    fontWeight: 600,
    color: '#4a4a6a',
    marginBottom: '0.35rem',
  } as React.CSSProperties,
  formControl: {
    height: 44,
    border: '1.5px solid #e2e2f0',
    borderRadius: 8,
    fontSize: '0.92rem',
    padding: '0.5rem 0.85rem',
    background: '#fafaff',
  } as React.CSSProperties,
  errorAlert: {
    border: 'none',
    borderRadius: 8,
    padding: '0.65rem 0.9rem',
    fontSize: '0.88rem',
    background: '#fff0f0',
    color: '#c0392b',
    borderLeft: '4px solid #e55353',
    marginBottom: '0.75rem',
  } as React.CSSProperties,
  optionsRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: '0.3rem',
    marginBottom: '1rem',
  } as React.CSSProperties,
  forgotLink: {
    fontSize: '0.85rem',
    color: '#667eea',
    textDecoration: 'none',
    fontWeight: 600,
    whiteSpace: 'nowrap',
  } as React.CSSProperties,
  divider: {
    display: 'flex',
    alignItems: 'center',
    margin: '0.25rem 0 0.85rem',
  } as React.CSSProperties,
  dividerLine: {
    flex: 1,
    height: 1,
    background: '#e8e8f0',
  } as React.CSSProperties,
  dividerText: {
    padding: '0 14px',
    fontSize: '0.8rem',
    color: '#b0b0c0',
    whiteSpace: 'nowrap',
  } as React.CSSProperties,
  registerBtn: {
    borderRadius: 8,
    fontSize: '0.9rem',
    fontWeight: 500,
    height: 42,
    borderColor: '#d8d8e8',
    color: '#667eea',
    width: '100%',
  } as React.CSSProperties,
  modalFooter: {
    borderTop: '1px solid #f0f0f5',
    padding: '1rem 1.5rem',
    gap: '0.75rem',
  } as React.CSSProperties,
  cancelBtn: {
    borderRadius: 8,
    fontSize: '0.9rem',
    padding: '0.45rem 1.4rem',
  } as React.CSSProperties,
  submitBtn: {
    borderRadius: 8,
    fontSize: '0.9rem',
    fontWeight: 600,
    padding: '0.45rem 2rem',
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    border: 'none',
    letterSpacing: 2,
  } as React.CSSProperties,
};

const LoginModal = (props: ILoginModalProps) => {
  const login = ({ username, password, rememberMe }) => {
    props.handleLogin(username, password, rememberMe);
  };

  const {
    handleSubmit,
    register,
    formState: { errors, touchedFields },
  } = useForm({ mode: 'onTouched' });

  const { loginError, handleClose } = props;

  const handleLoginSubmit = e => {
    handleSubmit(login)(e);
  };

  return (
    <Modal
      isOpen={props.showModal}
      toggle={handleClose}
      backdrop="static"
      id="login-page"
      autoFocus={false}
      contentClassName="border-0"
      style={{ maxWidth: 430 }}
    >
      {/* ---- Header ---- */}
      <ModalHeader id="login-title" data-cy="loginTitle" toggle={handleClose} style={styles.modalHeader}>
        <div style={styles.brandArea}>
          <div style={styles.brandIcon}>EC</div>
          <div>
            <span style={styles.brandText}>EveryCent</span>
            <p style={styles.brandDesc}>欢迎回来</p>
          </div>
        </div>
      </ModalHeader>

      <Form onSubmit={handleLoginSubmit}>
        {/* ---- Body ---- */}
        <ModalBody style={styles.modalBody}>
          <Row>
            <Col md="12">
              {loginError && (
                <Alert color="danger" data-cy="loginError" style={styles.errorAlert}>
                  <strong>登录失败!</strong> 请检查您的登录信息, 并重试一次.
                </Alert>
              )}
            </Col>
            <Col md="12">
              <ValidatedField
                name="username"
                label="账号"
                placeholder="请输入您的账号"
                required
                autoFocus
                data-cy="username"
                validate={{ required: '请输入账号' }}
                register={register}
                error={errors.username as FieldError}
                isTouched={touchedFields.username}
              />
              <ValidatedField
                name="password"
                type="password"
                label="密码"
                placeholder="请输入您的密码"
                required
                data-cy="password"
                validate={{ required: '请输入密码' }}
                register={register}
                error={errors.password as FieldError}
                isTouched={touchedFields.password}
              />
              <div style={styles.optionsRow}>
                <ValidatedField name="rememberMe" type="checkbox" check label="记住我" value={true} register={register} />
                <Link to="/account/reset/request" data-cy="forgetYourPasswordSelector" style={styles.forgotLink}>
                  忘记密码?
                </Link>
              </div>
            </Col>
          </Row>
          {/* 分割线 */}
          <div style={styles.divider}>
            <div style={styles.dividerLine} />
            <span style={styles.dividerText}>没有账号?</span>
            <div style={styles.dividerLine} />
          </div>
          <Link to="/account/register" className="btn btn-outline-primary" style={styles.registerBtn}>
            注册一个新账号
          </Link>
        </ModalBody>

        {/* ---- Footer ---- */}
        <ModalFooter style={styles.modalFooter}>
          <Button color="secondary" onClick={handleClose} tabIndex={1} outline style={styles.cancelBtn}>
            取消
          </Button>
          <Button color="primary" type="submit" data-cy="submit" style={styles.submitBtn}>
            登 录
          </Button>
        </ModalFooter>
      </Form>
    </Modal>
  );
};

export default LoginModal;
