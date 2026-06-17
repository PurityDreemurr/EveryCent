import React from 'react';

import { ConfirmDialog } from 'app/shared/components/everycent/overlays';

import TasksMutateDrawer from './tasks-mutate-drawer';
import { previewSubmittedData } from './submission-preview';
import { useTasks } from './tasks-provider';

const TasksDialogs = () => {
  const { open, setOpen, currentRow, setCurrentRow } = useTasks();

  const closeWithRowCleanup = () => {
    setOpen(null);
    window.setTimeout(() => setCurrentRow(null), 250);
  };

  return (
    <>
      <TasksMutateDrawer key="task-create" open={open === 'create'} onOpenChange={value => setOpen(value ? 'create' : null)} />

      {currentRow && (
        <>
          <TasksMutateDrawer
            key={`task-update-${currentRow.title}`}
            open={open === 'update'}
            onOpenChange={value => (value ? setOpen('update') : closeWithRowCleanup())}
            currentRow={currentRow}
          />
          <ConfirmDialog
            key={`task-delete-${currentRow.title}`}
            open={open === 'delete'}
            onOpenChange={value => (value ? setOpen('delete') : closeWithRowCleanup())}
            title={`删除这个任务：${currentRow.title}？`}
            desc={
              <>
                你即将删除一个优先级为 <strong>{currentRow.priority}</strong> 的任务。此操作无法撤销。
              </>
            }
            confirmText="删除"
            destructive
            handleConfirm={() => {
              previewSubmittedData('task-delete', currentRow);
              closeWithRowCleanup();
            }}
          />
        </>
      )}
    </>
  );
};

export default TasksDialogs;
