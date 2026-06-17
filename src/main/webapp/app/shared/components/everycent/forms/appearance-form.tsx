import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import type { EveryCentTheme } from 'app/shared/layout/everycent-shell/theme-provider';
import { useEveryCentTheme } from 'app/shared/layout/everycent-shell/theme-provider';
import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const appearanceSchema = z.object({
  theme: z.enum(['light', 'dark', 'system']),
  density: z.enum(['comfortable', 'compact']),
});

type AppearanceFormValues = z.infer<typeof appearanceSchema>;

const themeOptions: { label: string; previewClassName: string; value: EveryCentTheme }[] = [
  { value: 'light', label: 'light', previewClassName: 'ec-theme-preview ec-theme-preview--light' },
  { value: 'dark', label: 'dark', previewClassName: 'ec-theme-preview ec-theme-preview--dark' },
  { value: 'system', label: 'system', previewClassName: 'ec-theme-preview ec-theme-preview--system' },
];

const AppearanceForm = () => {
  const { setTheme, theme } = useEveryCentTheme();

  const { register, handleSubmit, watch } = useForm<AppearanceFormValues>({
    resolver: zodResolver(appearanceSchema),
    defaultValues: {
      theme,
      density: 'comfortable',
    },
  });

  const selectedTheme = watch('theme');
  const submitAppearance = (values: AppearanceFormValues) => {
    setTheme(values.theme);
    previewSubmittedData('appearance', values);
  };

  return (
    <form className="ec-form" onSubmit={handleSubmit(submitAppearance)}>
      <label className="ec-field">
        <span>Font density</span>
        <select {...register('density')}>
          <option value="comfortable">Comfortable</option>
          <option value="compact">Compact</option>
        </select>
      </label>

      <fieldset className="ec-theme-picker">
        <legend>Theme</legend>
        {themeOptions.map(option => (
          <label className={selectedTheme === option.value ? 'active' : ''} key={option.value}>
            <input type="radio" value={option.value} {...register('theme')} />
            <span className={option.previewClassName}>
              <i />
              <b />
              <b />
            </span>
            <strong>{option.label}</strong>
          </label>
        ))}
      </fieldset>

      <button className="ec-button ec-button--primary" type="submit">
        Update preferences
      </button>
    </form>
  );
};

export default AppearanceForm;
