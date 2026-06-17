import React from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const profileSchema = z.object({
  username: z.string().min(2, 'Username must be at least 2 characters.').max(30, 'Username must be 30 characters or less.'),
  email: z.string().min(1, 'Email is required.').email('Please enter a valid email.'),
  bio: z.string().min(4, 'Bio must be at least 4 characters.').max(160, 'Bio must be 160 characters or less.'),
  urls: z.array(z.object({ value: z.string().url('Please enter a valid URL.') })).optional(),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

const ProfileForm = () => {
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      username: 'everycent_user',
      email: 'user@everycent.local',
      bio: 'I use EveryCent to keep daily spending visible.',
      urls: [{ value: 'https://everycent.local' }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    name: 'urls',
    control,
  });

  return (
    <form className="ec-form" onSubmit={handleSubmit(values => previewSubmittedData('profile', values))}>
      <label className="ec-field">
        <span>Username</span>
        <input {...register('username')} />
        <em>This is your public display name in shared ledgers.</em>
        {errors.username && <small>{errors.username.message}</small>}
      </label>
      <label className="ec-field">
        <span>Email</span>
        <input type="email" {...register('email')} />
        <em>Used for account notifications and exports.</em>
        {errors.email && <small>{errors.email.message}</small>}
      </label>
      <label className="ec-field">
        <span>Bio</span>
        <textarea rows={4} {...register('bio')} />
        {errors.bio && <small>{errors.bio.message}</small>}
      </label>

      <div className="ec-field-group">
        <div>
          <span>URLs</span>
          <em>Add links that should appear on your profile.</em>
        </div>
        {fields.map((field, index) => (
          <label className="ec-field ec-field--inline" key={field.id}>
            <input {...register(`urls.${index}.value`)} />
            <button className="ec-button" type="button" onClick={() => remove(index)}>
              Remove
            </button>
            {errors.urls?.[index]?.value && <small>{errors.urls[index]?.value?.message}</small>}
          </label>
        ))}
        <button className="ec-button" type="button" onClick={() => append({ value: '' })}>
          Add URL
        </button>
      </div>

      <button className="ec-button ec-button--primary" type="submit">
        Update profile
      </button>
    </form>
  );
};

export default ProfileForm;
