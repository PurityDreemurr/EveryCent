import React, { useState } from 'react';
import { Table } from '@tanstack/react-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

type DataTableViewOptionsProps<TData> = {
  table: Table<TData>;
};

export function DataTableViewOptions<TData>({ table }: DataTableViewOptionsProps<TData>) {
  const [open, setOpen] = useState(false);
  const columns = table.getAllColumns().filter(column => typeof column.accessorFn !== 'undefined' && column.getCanHide());

  return (
    <div className="ec-data-table__view-options">
      <button type="button" onClick={() => setOpen(value => !value)}>
        <FontAwesomeIcon icon="eye" />
        View
      </button>
      {open && (
        <div className="ec-data-table__view-menu">
          <strong>Toggle columns</strong>
          {columns.map(column => (
            <label key={column.id}>
              <input type="checkbox" checked={column.getIsVisible()} onChange={event => column.toggleVisibility(event.target.checked)} />
              <span>{column.id}</span>
            </label>
          ))}
        </div>
      )}
    </div>
  );
}
