import React from 'react';
import { Column } from '@tanstack/react-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

type DataTableColumnHeaderProps<TData, TValue> = React.HTMLAttributes<HTMLDivElement> & {
  column: Column<TData, TValue>;
  title: string;
};

export function DataTableColumnHeader<TData, TValue>({ column, title, className = '' }: DataTableColumnHeaderProps<TData, TValue>) {
  if (!column.getCanSort()) {
    return <div className={className}>{title}</div>;
  }

  const sorted = column.getIsSorted();

  return (
    <div className={`ec-data-table__column-header ${className}`}>
      <button type="button" onClick={() => column.toggleSorting(sorted === 'asc')} title={`排序 ${title}`}>
        <span>{title}</span>
        {sorted === 'desc' ? (
          <FontAwesomeIcon icon="sort" className="ec-data-table__sort-icon is-desc" />
        ) : sorted === 'asc' ? (
          <FontAwesomeIcon icon="sort" className="ec-data-table__sort-icon is-asc" />
        ) : (
          <FontAwesomeIcon icon="sort" className="ec-data-table__sort-icon" />
        )}
      </button>
      {column.getCanHide() && (
        <button className="ec-data-table__hide-column" type="button" onClick={() => column.toggleVisibility(false)} title={`隐藏 ${title}`}>
          <FontAwesomeIcon icon="eye" />
        </button>
      )}
    </div>
  );
}
