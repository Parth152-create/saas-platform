import React from 'react';
import { Link } from 'react-router-dom';
import { CheckSquare, Calendar, ArrowRight } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import { Badge } from '../common/Badge';
import type { TaskItem } from '../../mocks/mockHrmData';
import { formatDate } from '../../utils/formatters';

export interface TaskListWidgetProps {
  tasks: TaskItem[];
  onToggleStatus?: (taskId: string) => void;
  className?: string;
  seeAllLink?: string;
}

export const TaskListWidget: React.FC<TaskListWidgetProps> = ({
  tasks,
  className = '',
  seeAllLink = '/app/tasks',
}) => {
  const pendingCount = tasks.filter((t) => t.status !== 'Completed').length;

  return (
    <Card className={`overflow-hidden min-w-0 ${className}`}>
      <CardHeader className="flex items-center justify-between gap-2.5 sm:gap-4 flex-wrap">
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <CheckSquare className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
          <CardTitle className="text-sm font-semibold">Operational Tasks</CardTitle>
        </div>
        <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
          <span className="text-[11px] font-semibold text-neutral-500 dark:text-neutral-400">
            {pendingCount} Pending
          </span>
          <Link
            to={seeAllLink}
            className="inline-flex items-center gap-1 text-xs font-medium text-neutral-600 hover:text-neutral-950 dark:text-neutral-400 dark:hover:text-neutral-100 transition-colors group"
          >
            <span>See all</span>
            <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
          {tasks.map((task) => (
            <div
              key={task.id}
              className="flex items-center justify-between p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors gap-3"
            >
              <div className="space-y-1 min-w-0 flex-1">
                <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                  {task.title}
                </p>
                <div className="flex items-center gap-2 text-[10px] text-neutral-400 flex-wrap">
                  <span className="truncate">Assignee: {task.assignee}</span>
                  <span>•</span>
                  <span className="flex items-center gap-1 shrink-0">
                    <Calendar className="w-3 h-3" />
                    Due {formatDate(task.dueDate)}
                  </span>
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
                  variant={
                    task.status === 'Completed'
                      ? 'success'
                      : task.status === 'In Progress'
                      ? 'info'
                      : 'default'
                  }
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
  );
};
