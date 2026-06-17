import React from 'react';

import UsersActionDialog from './users-action-dialog';
import UsersDeleteDialog from './users-delete-dialog';
import { useUsers } from './users-provider';

const UsersDialogs = () => {
  const { open, setOpen, currentRow, setCurrentRow } = useUsers();

  const closeWithRowCleanup = () => {
    setOpen(null);
    window.setTimeout(() => setCurrentRow(null), 250);
  };

  return (
    <>
      <UsersActionDialog key="user-add" open={open === 'add'} onOpenChange={value => setOpen(value ? 'add' : null)} />

      {currentRow && (
        <>
          <UsersActionDialog
            key={`user-edit-${currentRow.username}`}
            open={open === 'edit'}
            onOpenChange={value => (value ? setOpen('edit') : closeWithRowCleanup())}
            currentRow={currentRow}
          />
          <UsersDeleteDialog
            key={`user-delete-${currentRow.username}`}
            open={open === 'delete'}
            onOpenChange={value => (value ? setOpen('delete') : closeWithRowCleanup())}
            currentRow={currentRow}
          />
        </>
      )}
    </>
  );
};

export default UsersDialogs;
