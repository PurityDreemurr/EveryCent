import React, { createContext, useContext } from 'react';

import './overlays.scss';

type DialogContextValue = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

type DialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
};

type DialogContentProps = React.HTMLAttributes<HTMLElement> & {
  showCloseButton?: boolean;
};

const DialogContext = createContext<DialogContextValue | null>(null);

const useDialogContext = () => {
  const context = useContext(DialogContext);

  if (!context) {
    throw new Error('Dialog components must be used inside <Dialog>.');
  }

  return context;
};

export const Dialog = ({ open, onOpenChange, children }: DialogProps) => (
  <DialogContext.Provider value={{ open, onOpenChange }}>{children}</DialogContext.Provider>
);

export const DialogTrigger = ({ children }: { children: React.ReactElement }) => {
  const { onOpenChange } = useDialogContext();

  return React.cloneElement(children, {
    onClick(event: React.MouseEvent) {
      children.props.onClick?.(event);
      onOpenChange(true);
    },
  });
};

export const DialogClose = ({ children }: { children: React.ReactElement }) => {
  const { onOpenChange } = useDialogContext();

  return React.cloneElement(children, {
    onClick(event: React.MouseEvent) {
      children.props.onClick?.(event);
      onOpenChange(false);
    },
  });
};

export const DialogOverlay = () => {
  const { onOpenChange } = useDialogContext();

  return <div className="ec-overlay__backdrop" onClick={() => onOpenChange(false)} />;
};

export const DialogPortal = ({ children }: { children: React.ReactNode }) => <>{children}</>;

export const DialogContent = ({ children, className = '', showCloseButton = true, ...props }: DialogContentProps) => {
  const { open, onOpenChange } = useDialogContext();

  if (!open) return null;

  return (
    <div className="ec-overlay ec-overlay--dialog" role="presentation">
      <DialogOverlay />
      <section className={`ec-overlay__panel ec-overlay__panel--dialog ${className}`} role="dialog" aria-modal="true" {...props}>
        {children}
        {showCloseButton && (
          <button className="ec-overlay__close" type="button" aria-label="关闭" onClick={() => onOpenChange(false)}>
            x
          </button>
        )}
      </section>
    </div>
  );
};

export const DialogHeader = ({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={`ec-overlay__header ${className}`} {...props} />
);

export const DialogFooter = ({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={`ec-overlay__footer ${className}`} {...props} />
);

export const DialogTitle = ({ className = '', ...props }: React.HTMLAttributes<HTMLHeadingElement>) => (
  <h2 className={`ec-overlay__title ${className}`} {...props} />
);

export const DialogDescription = ({ className = '', ...props }: React.HTMLAttributes<HTMLParagraphElement>) => (
  <p className={`ec-overlay__description ${className}`} {...props} />
);
