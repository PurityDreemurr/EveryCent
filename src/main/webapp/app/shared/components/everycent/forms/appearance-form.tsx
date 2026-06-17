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
  { value: 'light', label: '浅色', previewClassName: 'ec-theme-preview ec-theme-preview--light' },
  { value: 'dark', label: '深色', previewClassName: 'ec-theme-preview ec-theme-preview--dark' },
  { value: 'system', label: '跟随系统', previewClassName: 'ec-theme-preview ec-theme-preview--system' },
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
        <span>字体密度</span>
        <select {...register('density')}>
          <option value="comfortable">舒适</option>
          <option value="compact">紧凑</option>
        </select>
      </label>

      <fieldset className="ec-theme-picker">
        <legend>主题</legend>
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
        更新偏好
      </button>
    </form>
  );
};

export default AppearanceForm;
