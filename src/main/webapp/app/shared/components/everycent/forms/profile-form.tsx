import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { loadSettings, saveSettings } from './settings-storage';
import './forms.scss';

const profileSchema = z.object({
  username: z.string().min(2, '用户名至少需要 2 个字符。').max(30, '用户名不能超过 30 个字符。'),
  email: z.string().min(1, '邮箱是必填项。').email('请输入有效的邮箱地址。'),
  bio: z.string().min(4, '简介至少需要 4 个字符。').max(160, '简介不能超过 160 个字符。'),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

const defaultProfileValues: ProfileFormValues = {
  username: 'everycent_user',
  email: 'user@everycent.local',
  bio: '我使用 EveryCent 记录每日消费。',
};

const ProfileForm = () => {
  const [saved, setSaved] = React.useState(false);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: loadSettings('profile', defaultProfileValues),
  });

  const handleSave = (values: ProfileFormValues) => {
    saveSettings('profile', values);
    setSaved(true);
  };

  return (
    <form className="ec-form" onSubmit={handleSubmit(handleSave)}>
      {saved && <div className="ec-form__success">资料已保存。</div>}
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

      <button className="ec-button ec-button--primary" type="submit">
        更新资料
      </button>
    </form>
  );
};

export default ProfileForm;
