import React from 'react';

import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
} from './dialog';

export const AlertDialog = Dialog;
export const AlertDialogTrigger = DialogTrigger;
export const AlertDialogPortal = DialogPortal;
export const AlertDialogOverlay = DialogOverlay;
export const AlertDialogContent = DialogContent;
export const AlertDialogHeader = DialogHeader;
export const AlertDialogFooter = DialogFooter;
export const AlertDialogTitle = DialogTitle;
export const AlertDialogDescription = DialogDescription;

export const AlertDialogAction = ({ className = '', ...props }: React.ButtonHTMLAttributes<HTMLButtonElement>) => (
  <button className={`ec-button ec-button--primary ${className}`} type="button" {...props} />
);

export const AlertDialogCancel = ({ children, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement>) => (
  <DialogClose>
    <button className="ec-button" type="button" {...props}>
      {children}
    </button>
  </DialogClose>
);
