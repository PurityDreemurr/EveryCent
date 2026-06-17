import React from 'react';
import { Table } from '@tanstack/react-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { DataTableFacetedFilter } from './faceted-filter';
import { DataTableViewOptions } from './view-options';

type DataTableToolbarProps<TData> = {
  table: Table<TData>;
  searchPlaceholder?: string;
  searchKey?: string;
  filters?: {
    columnId: string;
    title: string;
    options: {
      label: string;
      value: string;
    }[];
  }[];
};

export function DataTableToolbar<TData>({ table, searchPlaceholder = '筛选...', searchKey, filters = [] }: DataTableToolbarProps<TData>) {
  const isFiltered = table.getState().columnFilters.length > 0 || table.getState().globalFilter;

  return (
    <div className="ec-data-table__toolbar">
      <div className="ec-data-table__toolbar-main">
        <input
          value={searchKey ? ((table.getColumn(searchKey)?.getFilterValue() as string) ?? '') : (table.getState().globalFilter ?? '')}
          onChange={event => {
            if (searchKey) {
              table.getColumn(searchKey)?.setFilterValue(event.target.value);
            } else {
              table.setGlobalFilter(event.target.value);
            }
          }}
          placeholder={searchPlaceholder}
        />
        <div className="ec-data-table__toolbar-filters">
          {filters.map(filter => {
            const column = table.getColumn(filter.columnId);
            if (!column) {
              return null;
            }

            return <DataTableFacetedFilter key={filter.columnId} column={column} title={filter.title} options={filter.options} />;
          })}
        </div>
        {isFiltered && (
          <button
            className="ec-data-table__reset"
            type="button"
            onClick={() => {
              table.resetColumnFilters();
              table.setGlobalFilter('');
            }}
          >
            重置
            <FontAwesomeIcon icon="times-circle" />
          </button>
        )}
      </div>
      <DataTableViewOptions table={table} />
    </div>
  );
}
