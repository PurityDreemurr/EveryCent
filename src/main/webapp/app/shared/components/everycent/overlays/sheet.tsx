import React, { createContext, useContext } from 'react';

import './overlays.scss';

type SheetContextValue = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
};

type SheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  children: React.ReactNode;
};

type SheetContentProps = React.HTMLAttributes<HTMLElement> & {
  side?: 'top' | 'right' | 'bottom' | 'left';
};

const SheetContext = createContext<SheetContextValue | null>(null);

const useSheetContext = () => {
  const context = useContext(SheetContext);

  if (!context) {
    throw new Error('Sheet components must be used inside <Sheet>.');
  }

  return context;
};

export const Sheet = ({ open, onOpenChange, children }: SheetProps) => (
  <SheetContext.Provider value={{ open, onOpenChange }}>{children}</SheetContext.Provider>
);

export const SheetTrigger = ({ children }: { children: React.ReactElement }) => {
  const { onOpenChange } = useSheetContext();

  return React.cloneElement(children, {
    onClick(event: React.MouseEvent) {
      children.props.onClick?.(event);
      onOpenChange(true);
    },
  });
};

export const SheetClose = ({ children }: { children: React.ReactElement }) => {
  const { onOpenChange } = useSheetContext();

  return React.cloneElement(children, {
    onClick(event: React.MouseEvent) {
      children.props.onClick?.(event);
      onOpenChange(false);
    },
  });
};

export const SheetContent = ({ children, className = '', side = 'right', ...props }: SheetContentProps) => {
  const { open, onOpenChange } = useSheetContext();

  if (!open) return null;

  return (
    <div className="ec-overlay ec-overlay--sheet" role="presentation">
      <div className="ec-overlay__backdrop" onClick={() => onOpenChange(false)} />
      <aside
        className={`ec-overlay__panel ec-overlay__panel--sheet ec-overlay__panel--${side} ${className}`}
        role="dialog"
        aria-modal="true"
        {...props}
      >
        {children}
        <button className="ec-overlay__close" type="button" aria-label="Close" onClick={() => onOpenChange(false)}>
          x
        </button>
      </aside>
    </div>
  );
};

export const SheetHeader = ({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={`ec-overlay__header ${className}`} {...props} />
);

export const SheetFooter = ({ className = '', ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={`ec-overlay__footer ec-overlay__footer--stack ${className}`} {...props} />
);

export const SheetTitle = ({ className = '', ...props }: React.HTMLAttributes<HTMLHeadingElement>) => (
  <h2 className={`ec-overlay__title ${className}`} {...props} />
);

export const SheetDescription = ({ className = '', ...props }: React.HTMLAttributes<HTMLParagraphElement>) => (
  <p className={`ec-overlay__description ${className}`} {...props} />
);
