import React from 'react';
import { Table } from '@tanstack/react-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

type DataTablePaginationProps<TData> = {
  table: Table<TData>;
  className?: string;
};

const getPageNumbers = (currentPage: number, totalPages: number) => {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  if (currentPage <= 4) {
    return [1, 2, 3, 4, 5, '...', totalPages];
  }

  if (currentPage >= totalPages - 3) {
    return [1, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
  }

  return [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages];
};

export function DataTablePagination<TData>({ table, className = '' }: DataTablePaginationProps<TData>) {
  const currentPage = table.getState().pagination.pageIndex + 1;
  const totalPages = Math.max(table.getPageCount(), 1);
  const pageNumbers = getPageNumbers(currentPage, totalPages);

  return (
    <div className={`ec-data-table__pagination ${className}`}>
      <div className="ec-data-table__page-size">
        <select value={table.getState().pagination.pageSize} onChange={event => table.setPageSize(Number(event.target.value))}>
          {[10, 20, 30, 40, 50].map(pageSize => (
            <option key={pageSize} value={pageSize}>
              {pageSize}
            </option>
          ))}
        </select>
        <span>Rows per page</span>
      </div>

      <div className="ec-data-table__pager">
        <span className="ec-data-table__page-label">
          Page {currentPage} of {totalPages}
        </span>
        <button type="button" onClick={() => table.setPageIndex(0)} disabled={!table.getCanPreviousPage()} aria-label="Go to first page">
          <FontAwesomeIcon icon="sync" rotation={180} />
        </button>
        <button type="button" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()} aria-label="Go to previous page">
          <FontAwesomeIcon icon="arrow-left" />
        </button>
        {pageNumbers.map((pageNumber, index) =>
          pageNumber === '...' ? (
            <span key={`ellipsis-${index}`} className="ec-data-table__ellipsis">
              ...
            </span>
          ) : (
            <button
              key={pageNumber}
              type="button"
              className={currentPage === pageNumber ? 'active' : ''}
              onClick={() => table.setPageIndex((pageNumber as number) - 1)}
              aria-label={`Go to page ${pageNumber}`}
            >
              {pageNumber}
            </button>
          ),
        )}
        <button type="button" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()} aria-label="Go to next page">
          <FontAwesomeIcon icon="arrow-left" rotation={180} />
        </button>
        <button
          type="button"
          onClick={() => table.setPageIndex(table.getPageCount() - 1)}
          disabled={!table.getCanNextPage()}
          aria-label="Go to last page"
        >
          <FontAwesomeIcon icon="sync" />
        </button>
      </div>
    </div>
  );
}
