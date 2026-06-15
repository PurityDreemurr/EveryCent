import React, { useEffect } from 'react';
import { Button, Col, Row } from 'reactstrap';
import { ValidatedField, ValidatedForm, isEmail } from 'react-jhipster';
import { toast } from 'react-toastify';

import { useAppDispatch, useAppSelector } from 'app/config/store';
import { getSession } from 'app/shared/reducers/authentication';
import { reset, saveAccountSettings } from './settings.reducer';

export const SettingsPage = () => {
  const dispatch = useAppDispatch();
  const account = useAppSelector(state => state.authentication.account);
  const successMessage = useAppSelector(state => state.settings.successMessage);

  useEffect(() => {
    dispatch(getSession());
    return () => {
      dispatch(reset());
    };
  }, []);

  useEffect(() => {
    if (successMessage) {
      toast.success(successMessage);
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
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="settings-title">
            [<strong>{account.login}</strong>] 的用户设置
          </h2>
          <ValidatedForm id="settings-form" onSubmit={handleValidSubmit} defaultValues={account}>
            <ValidatedField
              name="firstName"
              label="名字"
              id="firstName"
              placeholder="您的名字"
              validate={{
                required: { value: true, message: '您的名字是必填项.' },
                minLength: { value: 1, message: '您的名字长度至少要有1个字符' },
                maxLength: { value: 50, message: '您的名字长度不能超过50个字符' },
              }}
              data-cy="firstname"
            />
            <ValidatedField
              name="lastName"
              label="姓氏"
              id="lastName"
              placeholder="您的姓氏"
              validate={{
                required: { value: true, message: '您的姓氏是必填项.' },
                minLength: { value: 1, message: '您的姓氏长度至少要有1个字符' },
                maxLength: { value: 50, message: '您的姓氏长度不能超过50个字符' },
              }}
              data-cy="lastname"
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
            <Button color="primary" type="submit" data-cy="submit">
              保存
            </Button>
          </ValidatedForm>
        </Col>
      </Row>
    </div>
  );
};

export default SettingsPage;
