import { useState } from 'react';
import { Plus } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { MOCK_TASKS, type TaskItem } from '../../mocks/mockHrmData';
import { formatDate } from '../../utils/formatters';
import { useToast } from '../../context/ToastContext';

export const TasksPage: React.FC = () => {
  const { showToast } = useToast();
  const [tasks, setTasks] = useState<TaskItem[]>(MOCK_TASKS);

  const toggleTask = (id: string) => {
    setTasks((prev) =>
      prev.map((t) =>
        t.id === id
          ? { ...t, status: t.status === 'Completed' ? 'In Progress' : 'Completed' }
          : t
      )
    );
    showToast('info', 'Task Updated', 'Task state saved.');
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Operational Tasks & Milestones"
        description="Track organizational deliverables, priority tickets, and assign work to teammates."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'Create Task', 'Task creation dialog')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Create Task
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle>Task Assignment Board</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
            {tasks.map((task) => (
              <div
                key={task.id}
                className="flex items-center justify-between p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
              >
                <div className="flex items-start gap-3 min-w-0 pr-4">
                  <input
                    type="checkbox"
                    checked={task.status === 'Completed'}
                    onChange={() => toggleTask(task.id)}
                    className="mt-1 h-4 w-4 rounded border-neutral-300 dark:border-[#262626] text-neutral-900 focus:ring-neutral-500 cursor-pointer accent-neutral-900 dark:accent-neutral-100"
                  />
                  <div>
                    <p
                      className={`text-xs font-semibold ${
                        task.status === 'Completed'
                          ? 'line-through text-neutral-400 dark:text-neutral-500'
                          : 'text-neutral-900 dark:text-neutral-100'
                      }`}
                    >
                      {task.title}
                    </p>
                    <p className="text-[11px] text-neutral-400 mt-0.5">
                      Assignee: <strong className="text-neutral-700 dark:text-neutral-300">{task.assignee}</strong> • Due {formatDate(task.dueDate)}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <Badge
                    variant={
                      task.priority === 'High'
                        ? 'danger'
                        : task.priority === 'Medium'
                        ? 'warning'
                        : 'default'
                    }
                    size="sm"
                  >
                    {task.priority}
                  </Badge>
                  <Badge
                    variant={task.status === 'Completed' ? 'success' : 'default'}
                    size="sm"
                  >
                    {task.status}
                  </Badge>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
