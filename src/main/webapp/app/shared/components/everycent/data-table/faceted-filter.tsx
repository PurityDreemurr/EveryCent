import React, { useState } from 'react';
import { Column } from '@tanstack/react-table';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

type FacetedOption = {
  label: string;
  value: string;
};

type DataTableFacetedFilterProps<TData, TValue> = {
  column?: Column<TData, TValue>;
  title?: string;
  options: FacetedOption[];
};

export function DataTableFacetedFilter<TData, TValue>({ column, title = '筛选', options }: DataTableFacetedFilterProps<TData, TValue>) {
  const [open, setOpen] = useState(false);
  const selectedValues = new Set((column?.getFilterValue() as string[]) ?? []);
  const selectedLabels = options.filter(option => selectedValues.has(option.value)).map(option => option.label);

  const toggleValue = (value: string) => {
    const nextValues = new Set(selectedValues);
    if (nextValues.has(value)) {
      nextValues.delete(value);
    } else {
      nextValues.add(value);
    }

    const filterValues = Array.from(nextValues);
    column?.setFilterValue(filterValues.length ? filterValues : undefined);
  };

  return (
    <div className="ec-data-table__filter">
      <button className="ec-data-table__filter-trigger" type="button" onClick={() => setOpen(value => !value)}>
        <FontAwesomeIcon icon="plus" />
        <span>{title}</span>
        {selectedValues.size > 0 && (
          <strong>{selectedValues.size > 2 ? `已选择 ${selectedValues.size} 项` : selectedLabels.join(', ')}</strong>
        )}
      </button>
      {open && (
        <div className="ec-data-table__filter-menu">
          {options.map(option => {
            const isSelected = selectedValues.has(option.value);
            const count = column?.getFacetedUniqueValues()?.get(option.value);

            return (
              <button key={option.value} type="button" className={isSelected ? 'active' : ''} onClick={() => toggleValue(option.value)}>
                <span className="ec-data-table__check">{isSelected && <FontAwesomeIcon icon="save" />}</span>
                <span>{option.label}</span>
                {count ? <small>{count}</small> : null}
              </button>
            );
          })}
          {selectedValues.size > 0 && (
            <button className="ec-data-table__clear-filter" type="button" onClick={() => column?.setFilterValue(undefined)}>
              清除筛选
            </button>
          )}
        </div>
      )}
    </div>
  );
}
