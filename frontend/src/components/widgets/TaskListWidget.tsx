import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { CheckSquare, Calendar, ArrowRight } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import { Badge } from '../common/Badge';
import { formatDate } from '../../utils/formatters';
import { projectsApi } from '../../api/projectsApi';

export interface GenericTaskItem {
  id: string;
  title: string;
  assignee?: string;
  assigneeName?: string | null;
  dueDate?: string | null;
  priority: string;
  status: string;
}

export interface TaskListWidgetProps {
  tasks?: GenericTaskItem[];
  onToggleStatus?: (taskId: string) => void;
  className?: string;
  seeAllLink?: string;
}

export const TaskListWidget: React.FC<TaskListWidgetProps> = ({
  tasks: propTasks,
  className = '',
  seeAllLink = '/app/tasks',
}) => {
  const [fetchedTasks, setFetchedTasks] = useState<GenericTaskItem[]>([]);
  const [isLoading, setIsLoading] = useState(!propTasks);

  useEffect(() => {
    if (propTasks) return;
    let isMounted = true;
    projectsApi.getAllTasks()
      .then((data) => {
        if (isMounted) {
          setFetchedTasks(data.slice(0, 5));
        }
      })
      .catch(() => {
        if (isMounted) setFetchedTasks([]);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [propTasks]);

  const tasks = propTasks || fetchedTasks;
  const pendingCount = tasks.filter((t) => t.status !== 'Completed' && t.status !== 'DONE').length;

  const getPriorityVariant = (priority: string) => {
    const p = priority.toUpperCase();
    if (p === 'HIGH' || p === 'URGENT') return 'danger';
    if (p === 'MEDIUM') return 'warning';
    return 'default';
  };

  const getStatusVariant = (status: string) => {
    const s = status.toUpperCase();
    if (s === 'COMPLETED' || s === 'DONE') return 'success';
    if (s === 'IN_PROGRESS' || s === 'IN PROGRESS') return 'info';
    return 'default';
  };

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
        {isLoading ? (
          <div className="p-4 space-y-2">
            <div className="h-4 bg-neutral-100 dark:bg-[#1a1a1a] rounded animate-pulse w-3/4" />
            <div className="h-4 bg-neutral-100 dark:bg-[#1a1a1a] rounded animate-pulse w-1/2" />
          </div>
        ) : tasks.length === 0 ? (
          <div className="p-8 text-center text-xs text-neutral-400">
            No operational tasks scheduled.
          </div>
        ) : (
          <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
            {tasks.map((task) => {
              const assigneeDisplay = task.assigneeName || task.assignee || 'Unassigned';
              const isDone = task.status === 'DONE' || task.status === 'Completed';

              return (
                <div
                  key={task.id}
                  className="flex items-center justify-between p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors gap-3"
                >
                  <div className="space-y-1 min-w-0 flex-1">
                    <p className={`text-xs font-semibold truncate ${isDone ? 'line-through text-neutral-400 dark:text-neutral-500' : 'text-neutral-900 dark:text-neutral-100'}`}>
                      {task.title}
                    </p>
                    <div className="flex items-center gap-2 text-[10px] text-neutral-400 flex-wrap">
                      <span className="truncate">Assignee: {assigneeDisplay}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1 shrink-0">
                        <Calendar className="w-3 h-3" />
                        Due {formatDate(task.dueDate)}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Badge variant={getPriorityVariant(task.priority)} size="sm">
                      {task.priority}
                    </Badge>
                    <Badge variant={getStatusVariant(task.status)} size="sm">
                      {task.status}
                    </Badge>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
