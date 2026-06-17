import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from 'app/shared/components/everycent/overlays';

import { EveryCentFormSubmit, EveryCentUserFormRecord } from './form-types';
import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const userSchema = z
  .object({
    firstName: z.string().min(1, '名是必填项。'),
    lastName: z.string().min(1, '姓是必填项。'),
    username: z.string().min(1, '用户名是必填项。'),
    phoneNumber: z.string().min(1, '手机号是必填项。'),
    email: z.string().min(1, '邮箱是必填项。').email('请输入有效的邮箱地址。'),
    role: z.string().min(1, '角色是必填项。'),
    password: z.string(),
    confirmPassword: z.string(),
    isEdit: z.boolean(),
  })
  .refine(values => values.isEdit || values.password.length > 0, {
    message: '密码是必填项。',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : values.password.length >= 8), {
    message: '密码至少需要 8 个字符。',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : /[a-z]/.test(values.password)), {
    message: '密码必须包含一个小写字母。',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : /\d/.test(values.password)), {
    message: '密码必须包含一个数字。',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : values.password === values.confirmPassword), {
    message: '两次输入的密码不一致。',
    path: ['confirmPassword'],
  });

type UserFormValues = z.infer<typeof userSchema>;

type UsersActionDialogProps = {
  open: boolean;
  currentRow?: EveryCentUserFormRecord;
  onOpenChange: (open: boolean) => void;
  onSubmit?: EveryCentFormSubmit<UserFormValues>;
};

const roles = [
  { label: '普通用户', value: 'user' },
  { label: '管理员', value: 'manager' },
  { label: '超级管理员', value: 'admin' },
];

const UsersActionDialog = ({ open, currentRow, onOpenChange, onSubmit }: UsersActionDialogProps) => {
  const isEdit = Boolean(currentRow);
  const {
    register,
    handleSubmit,
    watch,
    reset,
    formState: { errors },
  } = useForm<UserFormValues>({
    resolver: zodResolver(userSchema),
    values: {
      firstName: currentRow?.firstName ?? '',
      lastName: currentRow?.lastName ?? '',
      username: currentRow?.username ?? '',
      email: currentRow?.email ?? '',
      phoneNumber: currentRow?.phoneNumber ?? '',
      role: currentRow?.role ?? '',
      password: '',
      confirmPassword: '',
      isEdit,
    },
  });

  const close = () => {
    reset();
    onOpenChange(false);
  };

  const submit = (values: UserFormValues) => {
    onSubmit?.(values);
    previewSubmittedData('user', values);
    close();
  };

  const passwordTouched = watch('password').length > 0;

  return (
    <Dialog open={open} onOpenChange={value => (value ? onOpenChange(true) : close())}>
      <DialogContent aria-labelledby="ec-user-dialog-title">
        <DialogHeader>
          <DialogTitle id="ec-user-dialog-title">{isEdit ? '编辑用户' : '新增用户'}</DialogTitle>
          <DialogDescription>{isEdit ? '更新当前选中的用户。' : '创建一个新的用户账户。'}</DialogDescription>
        </DialogHeader>

        <form id="ec-user-form" className="ec-form ec-form--grid" onSubmit={handleSubmit(submit)}>
          <label className="ec-field">
            <span>名</span>
            <input {...register('firstName')} />
            {errors.firstName && <small>{errors.firstName.message}</small>}
          </label>
          <label className="ec-field">
            <span>姓</span>
            <input {...register('lastName')} />
            {errors.lastName && <small>{errors.lastName.message}</small>}
          </label>
          <label className="ec-field">
            <span>用户名</span>
            <input {...register('username')} />
            {errors.username && <small>{errors.username.message}</small>}
          </label>
          <label className="ec-field">
            <span>邮箱</span>
            <input type="email" {...register('email')} />
            {errors.email && <small>{errors.email.message}</small>}
          </label>
          <label className="ec-field">
            <span>手机号</span>
            <input {...register('phoneNumber')} />
            {errors.phoneNumber && <small>{errors.phoneNumber.message}</small>}
          </label>
          <label className="ec-field">
            <span>角色</span>
            <select {...register('role')}>
              <option value="">请选择角色</option>
              {roles.map(role => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
            {errors.role && <small>{errors.role.message}</small>}
          </label>
          <label className="ec-field">
            <span>密码</span>
            <input type="password" {...register('password')} />
            {errors.password && <small>{errors.password.message}</small>}
          </label>
          <label className="ec-field">
            <span>确认密码</span>
            <input type="password" disabled={!passwordTouched} {...register('confirmPassword')} />
            {errors.confirmPassword && <small>{errors.confirmPassword.message}</small>}
          </label>
        </form>

        <DialogFooter>
          <button className="ec-button" type="button" onClick={close}>
            取消
          </button>
          <button className="ec-button ec-button--primary" type="submit" form="ec-user-form">
            保存更改
          </button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default UsersActionDialog;
