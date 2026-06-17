import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const sidebarItems = [
  { id: 'ai', label: 'AI Record' },
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'ledger', label: 'Ledgers' },
  { id: 'transactions', label: 'Transactions' },
  { id: 'budget', label: 'Budgets' },
  { id: 'analytics', label: 'Analytics' },
] as const;

const displaySchema = z.object({
  items: z.array(z.string()).min(1, 'Select at least one item.'),
});

type DisplayFormValues = z.infer<typeof displaySchema>;

const DisplayForm = () => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<DisplayFormValues>({
    resolver: zodResolver(displaySchema),
    defaultValues: {
      items: ['ai', 'dashboard', 'ledger'],
    },
  });

  return (
    <form className="ec-form" onSubmit={handleSubmit(values => previewSubmittedData('display', values))}>
      <fieldset className="ec-checkbox-list">
        <legend>Sidebar</legend>
        <p>Select the items you want to display in the EveryCent sidebar.</p>
        {sidebarItems.map(item => (
          <label key={item.id}>
            <input type="checkbox" value={item.id} {...register('items')} />
            <span>{item.label}</span>
          </label>
        ))}
        {errors.items && <small>{errors.items.message}</small>}
      </fieldset>
      <button className="ec-button ec-button--primary" type="submit">
        Update display
      </button>
    </form>
  );
};

export default DisplayForm;
