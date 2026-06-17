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
      title="删除用户"
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
            你即将删除 <strong>{currentRow.username}</strong>。此操作无法撤销。
          </p>
          <label className="ec-field">
            <span>用户名</span>
            <input value={value} onChange={event => setValue(event.target.value)} placeholder="输入用户名以确认删除。" autoFocus />
          </label>
          <div className="ec-alert ec-alert--danger">
            <strong>警告</strong>
            <span>请谨慎操作，此操作无法回滚。</span>
          </div>
        </form>
      }
      confirmText="删除"
      destructive
    />
  );
};

export default UsersDeleteDialog;
