import React, { createContext, useContext, useState } from 'react';

import { EveryCentTaskFormRecord } from './form-types';
import { useDialogState } from './use-dialog-state';

type TasksDialogType = 'create' | 'update' | 'delete';

type TasksContextType = {
  open: TasksDialogType | null;
  setOpen: (value: TasksDialogType | null) => void;
  currentRow: EveryCentTaskFormRecord | null;
  setCurrentRow: React.Dispatch<React.SetStateAction<EveryCentTaskFormRecord | null>>;
};

const TasksContext = createContext<TasksContextType | null>(null);

export const TasksProvider = ({ children }: { children: React.ReactNode }) => {
  const [open, setOpen] = useDialogState<TasksDialogType>(null);
  const [currentRow, setCurrentRow] = useState<EveryCentTaskFormRecord | null>(null);

  return <TasksContext.Provider value={{ open, setOpen, currentRow, setCurrentRow }}>{children}</TasksContext.Provider>;
};

export const useTasks = () => {
  const context = useContext(TasksContext);

  if (!context) {
    throw new Error('useTasks must be used within <TasksProvider>.');
  }

  return context;
};
