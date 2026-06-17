import React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from 'app/shared/components/everycent/overlays';

import { EveryCentFormSubmit, EveryCentTaskFormRecord } from './form-types';
import { previewSubmittedData } from './submission-preview';
import './forms.scss';

const taskSchema = z.object({
  title: z.string().min(1, 'Title is required.'),
  status: z.string().min(1, 'Please select a status.'),
  label: z.string().min(1, 'Please select a label.'),
  priority: z.string().min(1, 'Please choose a priority.'),
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
          <SheetTitle id="ec-task-drawer-title">{isUpdate ? 'Update Task' : 'Create Task'}</SheetTitle>
          <SheetDescription>{isUpdate ? 'Update the task details.' : 'Add a task with status, label, and priority.'}</SheetDescription>
        </SheetHeader>

        <form id="ec-task-form" className="ec-form" onSubmit={handleSubmit(submit)}>
          <label className="ec-field">
            <span>Title</span>
            <input placeholder="Enter a title" {...register('title')} />
            {errors.title && <small>{errors.title.message}</small>}
          </label>
          <label className="ec-field">
            <span>Status</span>
            <select {...register('status')}>
              <option value="">Select status</option>
              <option value="in-progress">In Progress</option>
              <option value="backlog">Backlog</option>
              <option value="todo">Todo</option>
              <option value="done">Done</option>
              <option value="canceled">Canceled</option>
            </select>
            {errors.status && <small>{errors.status.message}</small>}
          </label>
          <fieldset className="ec-radio-group">
            <legend>Label</legend>
            {['documentation', 'feature', 'bug'].map(label => (
              <label key={label}>
                <input type="radio" value={label} {...register('label')} />
                <span>{label}</span>
              </label>
            ))}
            {errors.label && <small>{errors.label.message}</small>}
          </fieldset>
          <fieldset className="ec-radio-group">
            <legend>Priority</legend>
            {['high', 'medium', 'low'].map(priority => (
              <label key={priority}>
                <input type="radio" value={priority} {...register('priority')} />
                <span>{priority}</span>
              </label>
            ))}
            {errors.priority && <small>{errors.priority.message}</small>}
          </fieldset>
        </form>

        <SheetFooter>
          <button className="ec-button" type="button" onClick={close}>
            Close
          </button>
          <button className="ec-button ec-button--primary" type="submit" form="ec-task-form">
            Save changes
          </button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
};

export default TasksMutateDrawer;
