import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const notificationsSchema = z.object({
  type: z.enum(['all', 'important', 'none'], { message: '请选择通知范围。' }),
  mobile: z.boolean().optional(),
  budgetEmails: z.boolean().optional(),
  sharedLedgerEmails: z.boolean().optional(),
  weeklyReports: z.boolean().optional(),
  securityEmails: z.boolean(),
});

type NotificationsFormValues = z.infer<typeof notificationsSchema>;

const NotificationsForm = () => {
  const { register, handleSubmit } = useForm<NotificationsFormValues>({
    resolver: zodResolver(notificationsSchema),
    defaultValues: {
      type: 'important',
      mobile: false,
      budgetEmails: true,
      sharedLedgerEmails: true,
      weeklyReports: false,
      securityEmails: true,
    },
  });

  return (
    <form className="ec-form" onSubmit={handleSubmit(values => previewSubmittedData('notifications', values))}>
      <fieldset className="ec-radio-group">
        <legend>通知范围</legend>
        <label>
          <input type="radio" value="all" {...register('type')} />
          <span>全部活动</span>
        </label>
        <label>
          <input type="radio" value="important" {...register('type')} />
          <span>预算预警和共享账本更新</span>
        </label>
        <label>
          <input type="radio" value="none" {...register('type')} />
          <span>不通知</span>
        </label>
      </fieldset>

      <fieldset className="ec-switch-list">
        <legend>邮件通知</legend>
        <label>
          <span>
            <strong>预算邮件</strong>
            <em>当支出接近预算上限时接收提醒。</em>
          </span>
          <input type="checkbox" {...register('budgetEmails')} />
        </label>
        <label>
          <span>
            <strong>共享账本邮件</strong>
            <em>接收成员邀请和账本变更摘要。</em>
          </span>
          <input type="checkbox" {...register('sharedLedgerEmails')} />
        </label>
        <label>
          <span>
            <strong>周报</strong>
            <em>接收每周收支摘要。</em>
          </span>
          <input type="checkbox" {...register('weeklyReports')} />
        </label>
        <label>
          <span>
            <strong>安全邮件</strong>
            <em>始终接收重要账户安全通知。</em>
          </span>
          <input type="checkbox" disabled {...register('securityEmails')} />
        </label>
      </fieldset>

      <label className="ec-check-row">
        <input type="checkbox" {...register('mobile')} />
        <span>在移动设备上使用不同的通知设置。</span>
      </label>

      <button className="ec-button ec-button--primary" type="submit">
        更新通知
      </button>
    </form>
  );
};

export default NotificationsForm;
