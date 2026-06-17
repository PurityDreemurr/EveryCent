import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const notificationsSchema = z.object({
  type: z.enum(['all', 'important', 'none'], { message: 'Please select a notification type.' }),
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
        <legend>Notify me about</legend>
        <label>
          <input type="radio" value="all" {...register('type')} />
          <span>All activity</span>
        </label>
        <label>
          <input type="radio" value="important" {...register('type')} />
          <span>Budget warnings and shared ledger updates</span>
        </label>
        <label>
          <input type="radio" value="none" {...register('type')} />
          <span>Nothing</span>
        </label>
      </fieldset>

      <fieldset className="ec-switch-list">
        <legend>Email notifications</legend>
        <label>
          <span>
            <strong>Budget emails</strong>
            <em>Receive warnings when spending approaches a budget limit.</em>
          </span>
          <input type="checkbox" {...register('budgetEmails')} />
        </label>
        <label>
          <span>
            <strong>Shared ledger emails</strong>
            <em>Receive member invitations and ledger change summaries.</em>
          </span>
          <input type="checkbox" {...register('sharedLedgerEmails')} />
        </label>
        <label>
          <span>
            <strong>Weekly reports</strong>
            <em>Receive a weekly income and expense digest.</em>
          </span>
          <input type="checkbox" {...register('weeklyReports')} />
        </label>
        <label>
          <span>
            <strong>Security emails</strong>
            <em>Always receive important account security notices.</em>
          </span>
          <input type="checkbox" disabled {...register('securityEmails')} />
        </label>
      </fieldset>

      <label className="ec-check-row">
        <input type="checkbox" {...register('mobile')} />
        <span>Use different notification settings on mobile devices.</span>
      </label>

      <button className="ec-button ec-button--primary" type="submit">
        Update notifications
      </button>
    </form>
  );
};

export default NotificationsForm;
