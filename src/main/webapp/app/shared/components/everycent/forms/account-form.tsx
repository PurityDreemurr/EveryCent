import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const languages = [
  { label: 'English', value: 'en' },
  { label: 'Chinese', value: 'zh' },
  { label: 'Japanese', value: 'ja' },
  { label: 'Korean', value: 'ko' },
  { label: 'German', value: 'de' },
  { label: 'French', value: 'fr' },
];

const accountSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters.').max(30, 'Name must be 30 characters or less.'),
  dob: z.string().min(1, 'Please select your date of birth.'),
  language: z.string().min(1, 'Please select a language.'),
});

type AccountFormValues = z.infer<typeof accountSchema>;

const AccountForm = () => {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AccountFormValues>({
    resolver: zodResolver(accountSchema),
    defaultValues: {
      name: 'EveryCent User',
      dob: '',
      language: 'zh',
    },
  });

  return (
    <form className="ec-form" onSubmit={handleSubmit(values => previewSubmittedData('account', values))}>
      <label className="ec-field">
        <span>Name</span>
        <input placeholder="Your name" {...register('name')} />
        <em>This name appears in shared ledgers and invitations.</em>
        {errors.name && <small>{errors.name.message}</small>}
      </label>
      <label className="ec-field">
        <span>Date of birth</span>
        <input type="date" {...register('dob')} />
        {errors.dob && <small>{errors.dob.message}</small>}
      </label>
      <label className="ec-field">
        <span>Language</span>
        <select {...register('language')}>
          <option value="">Select language</option>
          {languages.map(language => (
            <option key={language.value} value={language.value}>
              {language.label}
            </option>
          ))}
        </select>
        {errors.language && <small>{errors.language.message}</small>}
      </label>
      <button className="ec-button ec-button--primary" type="submit">
        Update account
      </button>
    </form>
  );
};

export default AccountForm;
