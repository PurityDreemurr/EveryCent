import React, { useEffect, useState } from 'react';
import { ValidatedField, ValidatedForm, isEmail } from 'react-jhipster';
import { Alert, Button, Col, Row } from 'reactstrap';
import { toast } from 'react-toastify';
import { Link } from 'react-router-dom';

import PasswordStrengthBar from 'app/shared/layout/password/password-strength-bar';
import { useAppDispatch, useAppSelector } from 'app/config/store';
import { handleRegister, reset } from './register.reducer';

export const RegisterPage = () => {
  const [password, setPassword] = useState('');
  const dispatch = useAppDispatch();

  useEffect(
    () => () => {
      dispatch(reset());
    },
    [],
  );

  const handleValidSubmit = ({ username, email, firstPassword }) => {
    dispatch(handleRegister({ login: username, email, password: firstPassword, langKey: 'en' }));
  };

  const updatePassword = event => setPassword(event.target.value);

  const successMessage = useAppSelector(state => state.register.successMessage);

  useEffect(() => {
    if (successMessage) {
      toast.success(successMessage);
    }
  }, [successMessage]);

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h1 id="register-title" data-cy="registerTitle">
            注册
          </h1>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          <ValidatedForm id="register-form" onSubmit={handleValidSubmit}>
            <ValidatedField
              name="username"
              label="账号"
              placeholder="您的账号"
              validate={{
                required: { value: true, message: '您的账号是必填项.' },
                pattern: {
                  value: /^[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$|^[_.@A-Za-z0-9-]+$/,
                  message: 'Your username is invalid.',
                },
                minLength: { value: 1, message: '您的账号长度至少要有1个字符' },
                maxLength: { value: 50, message: '您的账号长度不能超过50个字符' },
              }}
              data-cy="username"
            />
            <ValidatedField
              name="email"
              label="电子邮件"
              placeholder="您的电子邮件"
              type="email"
              validate={{
                required: { value: true, message: '您的电子邮件是必填项.' },
                minLength: { value: 5, message: '您的电子邮件长度至少要有5个字符' },
                maxLength: { value: 254, message: '您的电子邮件长度不能超过50个字符' },
                validate: v => isEmail(v) || '您的电子邮件格式格式不正确.',
              }}
              data-cy="email"
            />
            <ValidatedField
              name="firstPassword"
              label="新密码"
              placeholder="您的新密码"
              type="password"
              onChange={updatePassword}
              validate={{
                required: { value: true, message: '您的密码是必填项.' },
                minLength: { value: 4, message: '您的密码长度至少要有4个字符' },
                maxLength: { value: 50, message: '您的密码长度不能超过50个字符' },
              }}
              data-cy="firstPassword"
            />
            <PasswordStrengthBar password={password} />
            <ValidatedField
              name="secondPassword"
              label="新密码确认"
              placeholder="确认您的新密码"
              type="password"
              validate={{
                required: { value: true, message: '您的确认密码是必填项.' },
                minLength: { value: 4, message: '您的确认密码长度至少要有4个字符' },
                maxLength: { value: 50, message: '您的确认密码长度不能超过50个字符' },
                validate: v => v === password || '您输入的密码和确认密码不匹配!',
              }}
              data-cy="secondPassword"
            />
            <Button id="register-submit" color="primary" type="submit" data-cy="submit">
              注册
            </Button>
          </ValidatedForm>
          <p>&nbsp;</p>
          <Alert color="warning">
            <span>如果您要 </span>
            <Link to="/login" className="alert-link">
              登录
            </Link>
            <span>
              , 您可以使用默认账号:
              <br />- 管理员 (账号=&quot;admin&quot;和密码=&quot;admin&quot;) <br />- 普通用户
              (账号=&quot;user&quot;和密码=&quot;user&quot;).
            </span>
          </Alert>
        </Col>
      </Row>
    </div>
  );
};

export default RegisterPage;
