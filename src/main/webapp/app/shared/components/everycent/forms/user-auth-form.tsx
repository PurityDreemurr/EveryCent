import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const authSchema = z.object({
  email: z.string().min(1, 'Email is required.').email('Please enter a valid email.'),
  password: z.string().min(1, 'Password is required.').min(7, 'Password must be at least 7 characters.'),
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
        <span>Email</span>
        <input type="email" placeholder="name@example.com" {...register('email')} />
        {errors.email && <small>{errors.email.message}</small>}
      </label>

      <label className="ec-field">
        <span>Password</span>
        <input type="password" placeholder="********" {...register('password')} />
        {errors.password && <small>{errors.password.message}</small>}
      </label>

      <button className="ec-button ec-button--primary" type="submit" disabled={isLoading}>
        {isLoading ? 'Signing in...' : 'Sign in'}
      </button>
    </form>
  );
};

export default UserAuthForm;
