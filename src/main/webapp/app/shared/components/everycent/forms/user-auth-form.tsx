import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const authSchema = z.object({
  email: z.string().min(1, '邮箱是必填项。').email('请输入有效的邮箱地址。'),
  password: z.string().min(1, '密码是必填项。').min(7, '密码至少需要 7 个字符。'),
});

type AuthFormValues = z.infer<typeof authSchema>;

type UserAuthFormProps = {
  onSubmit?: (values: AuthFormValues) => Promise<void> | void;
};

const UserAuthForm = ({ onSubmit }: UserAuthFormProps) => {
  const [isLoading, setIsLoading] = useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AuthFormValues>({
    resolver: zodResolver(authSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const submit = async (values: AuthFormValues) => {
    setIsLoading(true);
    try {
      await (onSubmit?.(values) ??
        Promise.resolve(previewSubmittedData('auth', { email: values.email, passwordLength: values.password.length })));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form className="ec-form ec-form--compact" onSubmit={handleSubmit(submit)}>
      <label className="ec-field">
        <span>邮箱</span>
        <input type="email" placeholder="name@example.com" {...register('email')} />
        {errors.email && <small>{errors.email.message}</small>}
      </label>

      <label className="ec-field">
        <span>密码</span>
        <input type="password" placeholder="********" {...register('password')} />
        {errors.password && <small>{errors.password.message}</small>}
      </label>

      <button className="ec-button ec-button--primary" type="submit" disabled={isLoading}>
        {isLoading ? '登录中...' : '登录'}
      </button>
    </form>
  );
};

export default UserAuthForm;
