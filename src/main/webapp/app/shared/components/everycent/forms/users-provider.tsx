import React, { createContext, useContext, useState } from 'react';

import { EveryCentUserFormRecord } from './form-types';
import { useDialogState } from './use-dialog-state';

type UsersDialogType = 'add' | 'edit' | 'delete';

type UsersContextType = {
  open: UsersDialogType | null;
  setOpen: (value: UsersDialogType | null) => void;
  currentRow: EveryCentUserFormRecord | null;
  setCurrentRow: React.Dispatch<React.SetStateAction<EveryCentUserFormRecord | null>>;
};

const UsersContext = createContext<UsersContextType | null>(null);

export const UsersProvider = ({ children }: { children: React.ReactNode }) => {
  const [open, setOpen] = useDialogState<UsersDialogType>(null);
  const [currentRow, setCurrentRow] = useState<EveryCentUserFormRecord | null>(null);

  return <UsersContext.Provider value={{ open, setOpen, currentRow, setCurrentRow }}>{children}</UsersContext.Provider>;
};

export const useUsers = () => {
  const context = useContext(UsersContext);

  if (!context) {
    throw new Error('useUsers must be used within <UsersProvider>.');
  }

  return context;
};
