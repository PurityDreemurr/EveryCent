import React from 'react';

import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from './alert-dialog';
import './overlays.scss';

type ConfirmDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: React.ReactNode;
  desc: React.ReactNode;
  cancelBtnText?: string;
  confirmText?: React.ReactNode;
  destructive?: boolean;
  disabled?: boolean;
  isLoading?: boolean;
  className?: string;
  children?: React.ReactNode;
} & ({ form: string; handleConfirm?: undefined } | { form?: undefined; handleConfirm: () => void });

export const ConfirmDialog = ({
  open,
  onOpenChange,
  title,
  desc,
  children,
  className = '',
  confirmText = 'Continue',
  cancelBtnText = '取消',
  destructive,
  disabled,
  isLoading,
  form,
  handleConfirm,
}: ConfirmDialogProps) => (
  <AlertDialog open={open} onOpenChange={onOpenChange}>
    <AlertDialogContent className={`ec-overlay__panel--confirm ${className}`}>
      <AlertDialogHeader>
        <AlertDialogTitle>{title}</AlertDialogTitle>
        <div className="ec-overlay__description">{desc}</div>
      </AlertDialogHeader>
      {children}
      <AlertDialogFooter>
        <button className="ec-button" type="button" disabled={isLoading} onClick={() => onOpenChange(false)}>
          {cancelBtnText}
        </button>
        <button
          className={`ec-button ${destructive ? 'ec-button--danger' : 'ec-button--primary'}`}
          type={form ? 'submit' : 'button'}
          form={form}
          onClick={handleConfirm}
          disabled={disabled || isLoading}
        >
          {confirmText}
        </button>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
);
