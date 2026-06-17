import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const sidebarItems = [
  { id: 'ai', label: 'AI 记账' },
  { id: 'dashboard', label: '仪表盘' },
  { id: 'ledger', label: '账本' },
  { id: 'transactions', label: '收支记录' },
  { id: 'budget', label: '预算' },
  { id: 'analytics', label: '数据分析' },
] as const;

const displaySchema = z.object({
  items: z.array(z.string()).min(1, '请至少选择一项。'),
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
        <legend>侧边栏</legend>
        <p>选择要显示在 EveryCent 侧边栏中的项目。</p>
        {sidebarItems.map(item => (
          <label key={item.id}>
            <input type="checkbox" value={item.id} {...register('items')} />
            <span>{item.label}</span>
          </label>
        ))}
        {errors.items && <small>{errors.items.message}</small>}
      </fieldset>
      <button className="ec-button ec-button--primary" type="submit">
        更新显示
      </button>
    </form>
  );
};

export default DisplayForm;
