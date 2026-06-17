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
    firstName: z.string().min(1, 'First name is required.'),
    lastName: z.string().min(1, 'Last name is required.'),
    username: z.string().min(1, 'Username is required.'),
    phoneNumber: z.string().min(1, 'Phone number is required.'),
    email: z.string().min(1, 'Email is required.').email('Please enter a valid email.'),
    role: z.string().min(1, 'Role is required.'),
    password: z.string(),
    confirmPassword: z.string(),
    isEdit: z.boolean(),
  })
  .refine(values => values.isEdit || values.password.length > 0, {
    message: 'Password is required.',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : values.password.length >= 8), {
    message: 'Password must be at least 8 characters.',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : /[a-z]/.test(values.password)), {
    message: 'Password must contain a lowercase letter.',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : /\d/.test(values.password)), {
    message: 'Password must contain a number.',
    path: ['password'],
  })
  .refine(values => (values.isEdit && !values.password ? true : values.password === values.confirmPassword), {
    message: "Passwords don't match.",
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
  { label: 'User', value: 'user' },
  { label: 'Manager', value: 'manager' },
  { label: 'Admin', value: 'admin' },
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
          <DialogTitle id="ec-user-dialog-title">{isEdit ? 'Edit User' : 'Add New User'}</DialogTitle>
          <DialogDescription>{isEdit ? 'Update the selected user.' : 'Create a new user account.'}</DialogDescription>
        </DialogHeader>

        <form id="ec-user-form" className="ec-form ec-form--grid" onSubmit={handleSubmit(submit)}>
          <label className="ec-field">
            <span>First name</span>
            <input {...register('firstName')} />
            {errors.firstName && <small>{errors.firstName.message}</small>}
          </label>
          <label className="ec-field">
            <span>Last name</span>
            <input {...register('lastName')} />
            {errors.lastName && <small>{errors.lastName.message}</small>}
          </label>
          <label className="ec-field">
            <span>Username</span>
            <input {...register('username')} />
            {errors.username && <small>{errors.username.message}</small>}
          </label>
          <label className="ec-field">
            <span>Email</span>
            <input type="email" {...register('email')} />
            {errors.email && <small>{errors.email.message}</small>}
          </label>
          <label className="ec-field">
            <span>Phone number</span>
            <input {...register('phoneNumber')} />
            {errors.phoneNumber && <small>{errors.phoneNumber.message}</small>}
          </label>
          <label className="ec-field">
            <span>Role</span>
            <select {...register('role')}>
              <option value="">Select role</option>
              {roles.map(role => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </select>
            {errors.role && <small>{errors.role.message}</small>}
          </label>
          <label className="ec-field">
            <span>Password</span>
            <input type="password" {...register('password')} />
            {errors.password && <small>{errors.password.message}</small>}
          </label>
          <label className="ec-field">
            <span>Confirm password</span>
            <input type="password" disabled={!passwordTouched} {...register('confirmPassword')} />
            {errors.confirmPassword && <small>{errors.confirmPassword.message}</small>}
          </label>
        </form>

        <DialogFooter>
          <button className="ec-button" type="button" onClick={close}>
            Cancel
          </button>
          <button className="ec-button ec-button--primary" type="submit" form="ec-user-form">
            Save changes
          </button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default UsersActionDialog;
