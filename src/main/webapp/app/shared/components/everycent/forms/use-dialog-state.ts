import { useState } from 'react';

export const useDialogState = <T extends string>(initialValue: T | null = null) => {
  const [open, setOpenState] = useState<T | null>(initialValue);

  const setOpen = (value: T | null) => {
    setOpenState(current => (current === value ? null : value));
  };

  return [open, setOpen] as const;
};
