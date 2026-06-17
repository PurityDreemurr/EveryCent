import React, { useEffect, useRef, useState } from 'react';
import { Table } from '@tanstack/react-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

type DataTableBulkActionsProps<TData> = {
  table: Table<TData>;
  entityName: string;
  children: React.ReactNode;
};

export function DataTableBulkActions<TData>({ table, entityName, children }: DataTableBulkActionsProps<TData>): React.ReactNode | null {
  const selectedRows = table.getFilteredSelectedRowModel().rows;
  const selectedCount = selectedRows.length;
  const toolbarRef = useRef<HTMLDivElement>(null);
  const [announcement, setAnnouncement] = useState('');

  useEffect(() => {
    if (selectedCount > 0) {
      const message = `${selectedCount} ${entityName}${selectedCount > 1 ? 's' : ''} selected. Bulk actions toolbar is available.`;
      setAnnouncement(message);
      const timer = setTimeout(() => setAnnouncement(''), 3000);
      return () => clearTimeout(timer);
    }

    return undefined;
  }, [selectedCount, entityName]);

  const handleClearSelection = () => table.resetRowSelection();

  const handleKeyDown = (event: React.KeyboardEvent) => {
    const buttons = toolbarRef.current?.querySelectorAll('button');
    if (!buttons) {
      return;
    }

    const currentIndex = Array.from(buttons).findIndex(button => button === document.activeElement);

    if (event.key === 'ArrowRight') {
      event.preventDefault();
      buttons[(currentIndex + 1) % buttons.length]?.focus();
    }

    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      buttons[currentIndex <= 0 ? buttons.length - 1 : currentIndex - 1]?.focus();
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      handleClearSelection();
    }
  };

  if (selectedCount === 0) {
    return null;
  }

  return (
    <>
      <div aria-live="polite" aria-atomic="true" className="sr-only" role="status">
        {announcement}
      </div>
      <div
        ref={toolbarRef}
        className="ec-data-table__bulk-actions"
        role="toolbar"
        aria-label={`Bulk actions for ${selectedCount} selected ${entityName}${selectedCount > 1 ? 's' : ''}`}
        tabIndex={-1}
        onKeyDown={handleKeyDown}
      >
        <button type="button" onClick={handleClearSelection} aria-label="Clear selection" title="Clear selection">
          <FontAwesomeIcon icon="times-circle" />
        </button>
        <strong>{selectedCount}</strong>
        <span>
          {entityName}
          {selectedCount > 1 ? 's' : ''} selected
        </span>
        {children}
      </div>
    </>
  );
}
