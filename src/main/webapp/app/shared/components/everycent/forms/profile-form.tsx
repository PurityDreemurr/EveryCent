import React from 'react';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const profileSchema = z.object({
  username: z.string().min(2, '用户名至少需要 2 个字符。').max(30, '用户名不能超过 30 个字符。'),
  email: z.string().min(1, '邮箱是必填项。').email('请输入有效的邮箱地址。'),
  bio: z.string().min(4, '简介至少需要 4 个字符。').max(160, '简介不能超过 160 个字符。'),
  urls: z.array(z.object({ value: z.string().url('请输入有效的网址。') })).optional(),
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
      bio: '我使用 EveryCent 记录每日消费。',
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
        <span>用户名</span>
        <input {...register('username')} />
        <em>这会作为你在共享账本中的公开名称。</em>
        {errors.username && <small>{errors.username.message}</small>}
      </label>
      <label className="ec-field">
        <span>邮箱</span>
        <input type="email" {...register('email')} />
        <em>用于接收账户通知和导出结果。</em>
        {errors.email && <small>{errors.email.message}</small>}
      </label>
      <label className="ec-field">
        <span>简介</span>
        <textarea rows={4} {...register('bio')} />
        {errors.bio && <small>{errors.bio.message}</small>}
      </label>

      <div className="ec-field-group">
        <div>
          <span>网址</span>
          <em>添加会显示在个人资料中的链接。</em>
        </div>
        {fields.map((field, index) => (
          <label className="ec-field ec-field--inline" key={field.id}>
            <input {...register(`urls.${index}.value`)} />
            <button className="ec-button" type="button" onClick={() => remove(index)}>
              删除
            </button>
            {errors.urls?.[index]?.value && <small>{errors.urls[index]?.value?.message}</small>}
          </label>
        ))}
        <button className="ec-button" type="button" onClick={() => append({ value: '' })}>
          添加网址
        </button>
      </div>

      <button className="ec-button ec-button--primary" type="submit">
        更新资料
      </button>
    </form>
  );
};

export default ProfileForm;
