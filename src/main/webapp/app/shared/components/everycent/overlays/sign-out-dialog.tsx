import React from 'react';
import { useNavigate } from 'react-router';

import { logout } from 'app/shared/reducers/authentication';
import { useAppDispatch } from 'app/config/store';

import { ConfirmDialog } from './confirm-dialog';

type SignOutDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

export const SignOutDialog = ({ open, onOpenChange }: SignOutDialogProps) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();

  const handleSignOut = () => {
    dispatch(logout());
    onOpenChange(false);
    navigate('/login');
  };

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title="退出登录"
      desc="确定要退出登录吗？再次访问账户时需要重新登录。"
      confirmText="退出登录"
      destructive
      handleConfirm={handleSignOut}
    />
  );
};
