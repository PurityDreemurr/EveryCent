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
      title="Sign out"
      desc="Are you sure you want to sign out? You will need to sign in again to access your account."
      confirmText="Sign out"
      destructive
      handleConfirm={handleSignOut}
    />
  );
};
