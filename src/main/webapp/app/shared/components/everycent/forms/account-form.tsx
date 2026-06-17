import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const languages = [
  { label: '英语', value: 'en' },
  { label: '中文', value: 'zh' },
  { label: '日语', value: 'ja' },
  { label: '韩语', value: 'ko' },
  { label: '德语', value: 'de' },
  { label: '法语', value: 'fr' },
];

const accountSchema = z.object({
  name: z.string().min(2, '姓名至少需要 2 个字符。').max(30, '姓名不能超过 30 个字符。'),
  dob: z.string().min(1, '请选择出生日期。'),
  language: z.string().min(1, '请选择一种语言。'),
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
      name: 'EveryCent 用户',
      dob: '',
      language: 'zh',
    },
  });

  return (
    <form className="ec-form" onSubmit={handleSubmit(values => previewSubmittedData('account', values))}>
      <label className="ec-field">
        <span>姓名</span>
        <input placeholder="你的姓名" {...register('name')} />
        <em>这个名字会显示在共享账本和邀请中。</em>
        {errors.name && <small>{errors.name.message}</small>}
      </label>
      <label className="ec-field">
        <span>出生日期</span>
        <input type="date" {...register('dob')} />
        {errors.dob && <small>{errors.dob.message}</small>}
      </label>
      <label className="ec-field">
        <span>语言</span>
        <select {...register('language')}>
          <option value="">请选择语言</option>
          {languages.map(language => (
            <option key={language.value} value={language.value}>
              {language.label}
            </option>
          ))}
        </select>
        {errors.language && <small>{errors.language.message}</small>}
      </label>
      <button className="ec-button ec-button--primary" type="submit">
        更新账户
      </button>
    </form>
  );
};

export default AccountForm;
