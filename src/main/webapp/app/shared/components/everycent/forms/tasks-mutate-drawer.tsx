import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from 'app/shared/components/everycent/overlays';

import { EveryCentFormSubmit, EveryCentTaskFormRecord } from './form-types';
import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const taskSchema = z.object({
  title: z.string().min(1, '标题是必填项。'),
  status: z.string().min(1, '请选择状态。'),
  label: z.string().min(1, '请选择标签。'),
  priority: z.string().min(1, '请选择优先级。'),
});

type TaskFormValues = z.infer<typeof taskSchema>;

type TasksMutateDrawerProps = {
  open: boolean;
  currentRow?: EveryCentTaskFormRecord;
  onOpenChange: (open: boolean) => void;
  onSubmit?: EveryCentFormSubmit<TaskFormValues>;
};

const TasksMutateDrawer = ({ open, currentRow, onOpenChange, onSubmit }: TasksMutateDrawerProps) => {
  const isUpdate = Boolean(currentRow);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TaskFormValues>({
    resolver: zodResolver(taskSchema),
    values: currentRow ?? {
      title: '',
      status: '',
      label: '',
      priority: '',
    },
  });

  const close = () => {
    reset();
    onOpenChange(false);
  };

  const submit = (values: TaskFormValues) => {
    onSubmit?.(values);
    previewSubmittedData('task', values);
    close();
  };

  return (
    <Sheet open={open} onOpenChange={value => (value ? onOpenChange(true) : close())}>
      <SheetContent aria-labelledby="ec-task-drawer-title">
        <SheetHeader>
          <SheetTitle id="ec-task-drawer-title">{isUpdate ? '更新任务' : '新建任务'}</SheetTitle>
          <SheetDescription>{isUpdate ? '更新任务详情。' : '添加一个包含状态、标签和优先级的任务。'}</SheetDescription>
        </SheetHeader>

        <form id="ec-task-form" className="ec-form" onSubmit={handleSubmit(submit)}>
          <label className="ec-field">
            <span>标题</span>
            <input placeholder="请输入标题" {...register('title')} />
            {errors.title && <small>{errors.title.message}</small>}
          </label>
          <label className="ec-field">
            <span>状态</span>
            <select {...register('status')}>
              <option value="">请选择状态</option>
              <option value="in-progress">进行中</option>
              <option value="backlog">待处理</option>
              <option value="todo">待办</option>
              <option value="done">已完成</option>
              <option value="canceled">已取消</option>
            </select>
            {errors.status && <small>{errors.status.message}</small>}
          </label>
          <fieldset className="ec-radio-group">
            <legend>标签</legend>
            {['documentation', 'feature', 'bug'].map(label => (
              <label key={label}>
                <input type="radio" value={label} {...register('label')} />
                <span>{label === 'documentation' ? '文档' : label === 'feature' ? '功能' : '缺陷'}</span>
              </label>
            ))}
            {errors.label && <small>{errors.label.message}</small>}
          </fieldset>
          <fieldset className="ec-radio-group">
            <legend>优先级</legend>
            {['high', 'medium', 'low'].map(priority => (
              <label key={priority}>
                <input type="radio" value={priority} {...register('priority')} />
                <span>{priority === 'high' ? '高' : priority === 'medium' ? '中' : '低'}</span>
              </label>
            ))}
            {errors.priority && <small>{errors.priority.message}</small>}
          </fieldset>
        </form>

        <SheetFooter>
          <button className="ec-button" type="button" onClick={close}>
            关闭
          </button>
          <button className="ec-button ec-button--primary" type="submit" form="ec-task-form">
            保存更改
          </button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
};

export default TasksMutateDrawer;
