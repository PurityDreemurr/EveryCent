import React, { useState } from 'react';

import { ConfirmDialog } from 'app/shared/components/everycent/overlays';

import { EveryCentUserFormRecord } from './form-types';
import { previewSubmittedData } from './submission-preview';
import './forms.scss';

type UsersDeleteDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  currentRow: EveryCentUserFormRecord;
};

const UsersDeleteDialog = ({ open, onOpenChange, currentRow }: UsersDeleteDialogProps) => {
  const [value, setValue] = useState('');

  const handleDelete = () => {
    if (value.trim() !== currentRow.username) return;

    previewSubmittedData('user-delete', currentRow);
    setValue('');
    onOpenChange(false);
  };

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={nextOpen => {
        if (!nextOpen) setValue('');
        onOpenChange(nextOpen);
      }}
      form="ec-users-delete-form"
      disabled={value.trim() !== currentRow.username}
      title="Delete User"
      desc={
        <form
          id="ec-users-delete-form"
          className="ec-form"
          onSubmit={event => {
            event.preventDefault();
            handleDelete();
          }}
        >
          <p>
            You are about to delete <strong>{currentRow.username}</strong>. This action cannot be undone.
          </p>
          <label className="ec-field">
            <span>Username</span>
            <input
              value={value}
              onChange={event => setValue(event.target.value)}
              placeholder="Enter username to confirm deletion."
              autoFocus
            />
          </label>
          <div className="ec-alert ec-alert--danger">
            <strong>Warning</strong>
            <span>Please be careful. This operation cannot be rolled back.</span>
          </div>
        </form>
      }
      confirmText="Delete"
      destructive
    />
  );
};

export default UsersDeleteDialog;
