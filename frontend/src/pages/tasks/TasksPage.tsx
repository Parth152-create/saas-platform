import React, { useState, useEffect, useCallback } from 'react';
import {
  CheckSquare,
  Plus,
  Search,
  Edit2,
  Trash2,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card } from '../../components/common/Card';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { formatDate, formatDateTime } from '../../utils/formatters';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import {
  projectsApi,
  type TaskResponse,
  type ProjectResponse,
  type TaskStatus,
  type TaskPriority,
  type CreateTaskData,
} from '../../api/projectsApi';
import { hrmApi, type EmployeeDto } from '../../api/hrmApi';

const COLUMNS: { id: TaskStatus; label: string; bg: string }[] = [
  { id: 'TODO', label: 'To Do', bg: 'bg-neutral-100 dark:bg-[#1a1a1a]' },
  { id: 'IN_PROGRESS', label: 'In Progress', bg: 'bg-blue-50/40 dark:bg-blue-950/20' },
  { id: 'REVIEW', label: 'Review', bg: 'bg-amber-50/40 dark:bg-amber-950/20' },
  { id: 'DONE', label: 'Done', bg: 'bg-emerald-50/40 dark:bg-emerald-950/20' },
];

export const TasksPage: React.FC = () => {
  const { showToast } = useToast();
  const { hasRole } = useAuth();

  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeDto[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [projectFilter, setProjectFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<TaskResponse | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);

  // Form State
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [taskFormData, setTaskFormData] = useState<CreateTaskData>({
    title: '',
    description: '',
    status: 'TODO',
    priority: 'MEDIUM',
    assigneeId: '',
    dueDate: '',
    estimatedHours: 0,
    actualHours: 0,
  });

  // Drag-and-drop state
  const [draggedTaskId, setDraggedTaskId] = useState<string | null>(null);

  const canManage = hasRole('MANAGER');

  const fetchTasks = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await projectsApi.getAllTasks({
        projectId: projectFilter !== 'ALL' ? projectFilter : undefined,
        priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
        search: search.trim() || undefined,
      });
      setTasks(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load tasks';
      setError(msg);
      showToast('error', 'Error Loading Tasks', msg);
    } finally {
      setIsLoading(false);
    }
  }, [projectFilter, priorityFilter, search, showToast]);

  useEffect(() => {
    let isMounted = true;
    projectsApi.getAllTasks({
      projectId: projectFilter !== 'ALL' ? projectFilter : undefined,
      priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
      search: search.trim() || undefined,
    })
      .then((data) => {
        if (isMounted) setTasks(data);
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to load tasks';
          setError(msg);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [projectFilter, priorityFilter, search]);

  useEffect(() => {
    Promise.all([
      projectsApi.getProjects(),
      hrmApi.getEmployees().catch(() => []),
    ]).then(([projData, empData]) => {
      setProjects(projData);
      setEmployees(empData);
      if (projData.length > 0) {
        setSelectedProjectId((prev) => prev || projData[0].id);
      }
    }).catch(() => {});
  }, []);

  const handleOpenCreate = () => {
    setTaskFormData({
      title: '',
      description: '',
      status: 'TODO',
      priority: 'MEDIUM',
      assigneeId: '',
      dueDate: '',
      estimatedHours: 0,
      actualHours: 0,
    });
    if (projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].id);
    }
    setIsCreateOpen(true);
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProjectId) {
      showToast('warning', 'Project Required', 'Please select or create a project first.');
      return;
    }
    if (!taskFormData.title.trim()) return;

    try {
      await projectsApi.createTask(selectedProjectId, {
        ...taskFormData,
        assigneeId: taskFormData.assigneeId || undefined,
        estimatedHours: Number(taskFormData.estimatedHours) || 0,
        actualHours: Number(taskFormData.actualHours) || 0,
      });
      showToast('success', 'Task Created', `Task "${taskFormData.title}" added to board.`);
      setIsCreateOpen(false);
      fetchTasks();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create task';
      showToast('error', 'Creation Failed', msg);
    }
  };

  const handleOpenDetail = (task: TaskResponse) => {
    setSelectedTask(task);
    setIsEditMode(false);
    setTaskFormData({
      title: task.title,
      description: task.description || '',
      status: task.status,
      priority: task.priority,
      assigneeId: task.assigneeId || '',
      dueDate: task.dueDate || '',
      estimatedHours: task.estimatedHours,
      actualHours: task.actualHours,
    });
    setIsDetailOpen(true);
  };

  const handleUpdateTaskSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTask) return;

    try {
      const updated = await projectsApi.updateTask(selectedTask.id, {
        title: canManage ? taskFormData.title : undefined,
        description: canManage ? taskFormData.description : undefined,
        status: taskFormData.status,
        priority: canManage ? taskFormData.priority : undefined,
        assigneeId: canManage ? (taskFormData.assigneeId || undefined) : undefined,
        dueDate: canManage ? taskFormData.dueDate : undefined,
        estimatedHours: canManage ? Number(taskFormData.estimatedHours) : undefined,
        actualHours: Number(taskFormData.actualHours) || 0,
      });
      showToast('success', 'Task Updated', `Task "${updated.title}" saved.`);
      setSelectedTask(updated);
      setIsEditMode(false);
      fetchTasks();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update task';
      showToast('error', 'Update Failed', msg);
    }
  };

  const handleDeleteTask = async (taskId: string, title: string) => {
    if (!window.confirm(`Are you sure you want to delete task "${title}"?`)) return;
    try {
      await projectsApi.deleteTask(taskId);
      showToast('success', 'Task Deleted', `Task "${title}" removed.`);
      setIsDetailOpen(false);
      fetchTasks();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete task';
      showToast('error', 'Delete Failed', msg);
    }
  };

  // Drag and Drop handlers
  const handleDragStart = (e: React.DragEvent, taskId: string) => {
    e.dataTransfer.setData('text/plain', taskId);
    setDraggedTaskId(taskId);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  const handleDrop = async (e: React.DragEvent, newStatus: TaskStatus) => {
    e.preventDefault();
    const taskId = e.dataTransfer.getData('text/plain') || draggedTaskId;
    setDraggedTaskId(null);

    if (!taskId) return;
    const task = tasks.find((t) => t.id === taskId);
    if (!task || task.status === newStatus) return;

    // Optimistic UI update
    setTasks((prev) =>
      prev.map((t) => (t.id === taskId ? { ...t, status: newStatus } : t))
    );

    try {
      await projectsApi.updateTask(taskId, { status: newStatus });
      showToast('success', 'Task Moved', `Task moved to ${newStatus.replace('_', ' ')}.`);
      fetchTasks();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to move task';
      showToast('error', 'Update Failed', msg);
      fetchTasks(); // Revert on failure
    }
  };

  const getPriorityBadgeVariant = (priority: TaskPriority) => {
    switch (priority) {
      case 'URGENT':
      case 'HIGH':
        return 'danger';
      case 'MEDIUM':
        return 'warning';
      default:
        return 'default';
    }
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Operational Tasks & Milestones"
        description="Kanban workflow board for cross-functional project deliverables, ticket assignments, and milestone tracking."
        actions={
          canManage && (
            <Button
              size="sm"
              onClick={handleOpenCreate}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              Create Task
            </Button>
          )
        }
      />

      {error && (
        <ErrorAlert
          title="Failed to load tasks"
          message={error}
          onRetry={fetchTasks}
        />
      )}

      {/* Filter / Search Controls */}
      <Card>
        <div className="p-4 bg-neutral-50/50 dark:bg-[#141414] flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="relative w-full md:max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search tasks by title, description, assignee..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-lg border border-neutral-200 bg-white pl-9 pr-4 py-2 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500"
            />
          </div>

          <div className="flex items-center gap-3 w-full md:w-auto">
            <Select
              value={projectFilter}
              onChange={(e) => setProjectFilter(e.target.value)}
              options={[
                { value: 'ALL', label: 'All Projects' },
                ...projects.map((p) => ({ value: p.id, label: p.name })),
              ]}
              className="w-full md:w-48 text-xs"
            />

            <Select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              options={[
                { value: 'ALL', label: 'All Priorities' },
                { value: 'LOW', label: 'Low' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'HIGH', label: 'High' },
                { value: 'URGENT', label: 'Urgent' },
              ]}
              className="w-full md:w-36 text-xs"
            />
          </div>
        </div>
      </Card>

      {/* Kanban Board Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] space-y-4">
              <LoadingSkeleton variant="card" />
              <LoadingSkeleton variant="card" />
            </div>
          ))}
        </div>
      ) : tasks.length === 0 && (projectFilter !== 'ALL' || priorityFilter !== 'ALL' || search) ? (
        <Card className="p-12 text-center">
          <EmptyState
            icon={<CheckSquare className="w-8 h-8" />}
            title="No tasks match your filters"
            description="Try resetting search or filter criteria to view all tasks on the board."
            actionLabel="Reset Filters"
            onAction={() => {
              setSearch('');
              setProjectFilter('ALL');
              setPriorityFilter('ALL');
            }}
            className="border-0 rounded-none bg-transparent"
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5">
          {COLUMNS.map((col) => {
            const columnTasks = tasks.filter((t) => t.status === col.id);

            return (
              <div
                key={col.id}
                onDragOver={handleDragOver}
                onDrop={(e) => handleDrop(e, col.id)}
                className="flex flex-col rounded-xl border border-neutral-200/80 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#111111] overflow-hidden min-h-[480px]"
              >
                {/* Column Header */}
                <div className="p-3.5 border-b border-neutral-200/70 dark:border-[#262626] flex items-center justify-between bg-white dark:bg-[#141414]">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                      {col.label}
                    </span>
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-neutral-100 dark:bg-[#222] text-[10px] font-bold text-neutral-600 dark:text-neutral-300">
                      {columnTasks.length}
                    </span>
                  </div>

                  {canManage && (
                    <button
                      type="button"
                      onClick={() => {
                        setTaskFormData((prev) => ({ ...prev, status: col.id }));
                        handleOpenCreate();
                      }}
                      className="p-1 rounded text-neutral-400 hover:text-neutral-800 dark:hover:text-neutral-100 hover:bg-neutral-100 dark:hover:bg-[#222] transition-colors"
                      title={`Add task to ${col.label}`}
                    >
                      <Plus className="w-3.5 h-3.5" />
                    </button>
                  )}
                </div>

                {/* Column Body / Cards List */}
                <div className="p-3 flex-1 space-y-3 overflow-y-auto">
                  {columnTasks.length === 0 ? (
                    <div className="h-36 flex flex-col items-center justify-center text-center p-4 border border-dashed border-neutral-200 dark:border-[#262626] rounded-lg">
                      <p className="text-[11px] text-neutral-400">No tasks in this lane</p>
                      <span className="text-[9.5px] text-neutral-400 mt-0.5">Drag tasks here</span>
                    </div>
                  ) : (
                    columnTasks.map((task) => {
                      const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && task.status !== 'DONE';

                      return (
                        <div
                          key={task.id}
                          draggable
                          onDragStart={(e) => handleDragStart(e, task.id)}
                          onClick={() => handleOpenDetail(task)}
                          className="group p-3.5 rounded-lg border border-neutral-200 bg-white dark:border-[#262626] dark:bg-[#161616] shadow-2xs hover:shadow-xs hover:border-neutral-300 dark:hover:border-neutral-700 transition-all cursor-grab active:cursor-grabbing space-y-2.5"
                        >
                          {/* Top: Project Name & Priority */}
                          <div className="flex items-center justify-between gap-2">
                            <span className="text-[10px] font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider truncate max-w-[140px]">
                              {task.projectName}
                            </span>
                            <Badge variant={getPriorityBadgeVariant(task.priority)} size="sm">
                              {task.priority}
                            </Badge>
                          </div>

                          {/* Title */}
                          <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 line-clamp-2">
                            {task.title}
                          </p>

                          {/* Footer: Assignee, Due Date */}
                          <div className="flex items-center justify-between pt-1 border-t border-neutral-100 dark:border-[#222] text-[10.5px] text-neutral-400">
                            <div className="flex items-center gap-1.5 min-w-0">
                              <div className="h-5 w-5 rounded-full bg-neutral-200 dark:bg-[#2a2a2a] flex items-center justify-center font-bold text-[9px] text-neutral-700 dark:text-neutral-200 shrink-0">
                                {task.assigneeName ? task.assigneeName.charAt(0).toUpperCase() : 'U'}
                              </div>
                              <span className="truncate max-w-[90px] text-neutral-600 dark:text-neutral-300">
                                {task.assigneeName || 'Unassigned'}
                              </span>
                            </div>

                            <span className={isOverdue ? 'text-rose-600 dark:text-rose-400 font-semibold' : ''}>
                              {formatDate(task.dueDate)}
                            </span>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Create Task Modal */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Create New Operational Task"
        size="md"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <Select
            label="Belongs to Project *"
            value={selectedProjectId}
            onChange={(e) => setSelectedProjectId(e.target.value)}
            options={[
              { value: '', label: 'Select project...' },
              ...projects.map((p) => ({ value: p.id, label: p.name })),
            ]}
            required
          />

          <Input
            label="Task Title *"
            value={taskFormData.title}
            onChange={(e) => setTaskFormData({ ...taskFormData, title: e.target.value })}
            placeholder="e.g. Audit tenant JWT expiration logic"
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Priority"
              value={taskFormData.priority || 'MEDIUM'}
              onChange={(e) => setTaskFormData({ ...taskFormData, priority: e.target.value as TaskPriority })}
              options={[
                { value: 'LOW', label: 'Low' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'HIGH', label: 'High' },
                { value: 'URGENT', label: 'Urgent' },
              ]}
            />
            <Select
              label="Initial Status"
              value={taskFormData.status || 'TODO'}
              onChange={(e) => setTaskFormData({ ...taskFormData, status: e.target.value as TaskStatus })}
              options={[
                { value: 'TODO', label: 'To Do' },
                { value: 'IN_PROGRESS', label: 'In Progress' },
                { value: 'REVIEW', label: 'Review' },
                { value: 'DONE', label: 'Done' },
              ]}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Assignee"
              value={taskFormData.assigneeId || ''}
              onChange={(e) => setTaskFormData({ ...taskFormData, assigneeId: e.target.value })}
              options={[
                { value: '', label: 'Unassigned' },
                ...employees.map((emp) => ({
                  value: emp.id,
                  label: `${emp.name} (${emp.department})`,
                })),
              ]}
            />
            <Input
              label="Due Date"
              type="date"
              value={taskFormData.dueDate || ''}
              onChange={(e) => setTaskFormData({ ...taskFormData, dueDate: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Estimated Hours"
              type="number"
              value={taskFormData.estimatedHours ?? 0}
              onChange={(e) => setTaskFormData({ ...taskFormData, estimatedHours: Number(e.target.value) })}
            />
            <Input
              label="Actual Hours Logged"
              type="number"
              value={taskFormData.actualHours ?? 0}
              onChange={(e) => setTaskFormData({ ...taskFormData, actualHours: Number(e.target.value) })}
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Description &amp; Acceptance Criteria
            </label>
            <textarea
              rows={3}
              value={taskFormData.description || ''}
              onChange={(e) => setTaskFormData({ ...taskFormData, description: e.target.value })}
              placeholder="What needs to be achieved in this deliverable..."
              className="w-full rounded-lg border border-neutral-200 bg-white p-2.5 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="outline" type="button" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">Create Task</Button>
          </div>
        </form>
      </Modal>

      {/* Task Detail Modal / Drawer (Section 13) */}
      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={isEditMode ? 'Edit Task' : 'Task Details'}
        size="lg"
      >
        {selectedTask && !isEditMode ? (
          <div className="space-y-5">
            {/* Header info */}
            <div className="flex items-start justify-between gap-4">
              <div>
                <span className="text-[10px] font-bold text-neutral-400 uppercase tracking-wider block">
                  Project: {selectedTask.projectName}
                </span>
                <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                  {selectedTask.title}
                </h3>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                <Badge variant={getPriorityBadgeVariant(selectedTask.priority)} size="sm">
                  {selectedTask.priority}
                </Badge>
                <Badge variant={selectedTask.status === 'DONE' ? 'success' : 'default'} size="sm" withDot>
                  {selectedTask.status}
                </Badge>
              </div>
            </div>

            {/* Description */}
            <div className="p-3.5 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
              <span className="text-[10px] font-semibold text-neutral-400 uppercase tracking-wider block mb-1">
                Description
              </span>
              <p className="text-xs text-neutral-700 dark:text-neutral-300 leading-relaxed whitespace-pre-wrap">
                {selectedTask.description || 'No detailed description provided.'}
              </p>
            </div>

            {/* Metadata Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
                <span className="text-neutral-400 block text-[10px] uppercase">Assignee</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100 mt-1 block">
                  {selectedTask.assigneeName || 'Unassigned'}
                </span>
              </div>

              <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
                <span className="text-neutral-400 block text-[10px] uppercase">Due Date</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100 mt-1 block">
                  {formatDate(selectedTask.dueDate)}
                </span>
              </div>

              <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
                <span className="text-neutral-400 block text-[10px] uppercase">Logged / Estimated</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100 mt-1 block">
                  {selectedTask.actualHours}h / {selectedTask.estimatedHours}h
                </span>
              </div>

              <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
                <span className="text-neutral-400 block text-[10px] uppercase">Created By</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100 mt-1 block">
                  {selectedTask.createdByName || 'System'}
                </span>
              </div>

              <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
                <span className="text-neutral-400 block text-[10px] uppercase">Created Date</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100 mt-1 block">
                  {formatDateTime(selectedTask.createdAt)}
                </span>
              </div>

              <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222]">
                <span className="text-neutral-400 block text-[10px] uppercase">Updated Date</span>
                <span className="font-semibold text-neutral-900 dark:text-neutral-100 mt-1 block">
                  {formatDateTime(selectedTask.updatedAt)}
                </span>
              </div>
            </div>

            {/* Quick Status Updater */}
            <div className="p-3.5 bg-neutral-50 dark:bg-[#141414] rounded-lg border border-neutral-100 dark:border-[#222] flex items-center justify-between gap-4">
              <span className="text-xs font-semibold text-neutral-700 dark:text-neutral-300">
                Change Status:
              </span>
              <div className="flex items-center gap-1.5">
                {(['TODO', 'IN_PROGRESS', 'REVIEW', 'DONE'] as TaskStatus[]).map((st) => (
                  <button
                    key={st}
                    type="button"
                    onClick={async () => {
                      try {
                        const updated = await projectsApi.updateTask(selectedTask.id, { status: st });
                        setSelectedTask(updated);
                        fetchTasks();
                        showToast('success', 'Status Changed', `Status updated to ${st}.`);
                      } catch (err: unknown) {
                        const msg = err instanceof Error ? err.message : 'Failed to update status';
                        showToast('error', 'Failed', msg);
                      }
                    }}
                    className={`px-2.5 py-1 text-[11px] rounded-md font-semibold transition-all ${
                      selectedTask.status === st
                        ? 'bg-neutral-900 text-white dark:bg-white dark:text-neutral-900'
                        : 'bg-white text-neutral-600 border border-neutral-200 hover:bg-neutral-100 dark:bg-[#202020] dark:text-neutral-300 dark:border-[#333]'
                    }`}
                  >
                    {st.replace('_', ' ')}
                  </button>
                ))}
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-between pt-4 border-t border-neutral-100 dark:border-[#262626]">
              {canManage && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleDeleteTask(selectedTask.id, selectedTask.title)}
                  className="text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30"
                  leftIcon={<Trash2 className="w-3.5 h-3.5" />}
                >
                  Delete Task
                </Button>
              )}

              <div className="flex items-center gap-2 ml-auto">
                <Button variant="secondary" size="sm" onClick={() => setIsEditMode(true)} leftIcon={<Edit2 className="w-3.5 h-3.5" />}>
                  Edit Details
                </Button>
                <Button size="sm" onClick={() => setIsDetailOpen(false)}>
                  Close
                </Button>
              </div>
            </div>
          </div>
        ) : selectedTask && isEditMode ? (
          <form onSubmit={handleUpdateTaskSubmit} className="space-y-4">
            {canManage && (
              <Input
                label="Task Title *"
                value={taskFormData.title}
                onChange={(e) => setTaskFormData({ ...taskFormData, title: e.target.value })}
                required
              />
            )}

            <div className="grid grid-cols-2 gap-4">
              <Select
                label="Status"
                value={taskFormData.status || 'TODO'}
                onChange={(e) => setTaskFormData({ ...taskFormData, status: e.target.value as TaskStatus })}
                options={[
                  { value: 'TODO', label: 'To Do' },
                  { value: 'IN_PROGRESS', label: 'In Progress' },
                  { value: 'REVIEW', label: 'Review' },
                  { value: 'DONE', label: 'Done' },
                ]}
              />
              {canManage && (
                <Select
                  label="Priority"
                  value={taskFormData.priority || 'MEDIUM'}
                  onChange={(e) => setTaskFormData({ ...taskFormData, priority: e.target.value as TaskPriority })}
                  options={[
                    { value: 'LOW', label: 'Low' },
                    { value: 'MEDIUM', label: 'Medium' },
                    { value: 'HIGH', label: 'High' },
                    { value: 'URGENT', label: 'Urgent' },
                  ]}
                />
              )}
            </div>

            {canManage && (
              <div className="grid grid-cols-2 gap-4">
                <Select
                  label="Assignee"
                  value={taskFormData.assigneeId || ''}
                  onChange={(e) => setTaskFormData({ ...taskFormData, assigneeId: e.target.value })}
                  options={[
                    { value: '', label: 'Unassigned' },
                    ...employees.map((emp) => ({
                      value: emp.id,
                      label: emp.name,
                    })),
                  ]}
                />
                <Input
                  label="Due Date"
                  type="date"
                  value={taskFormData.dueDate || ''}
                  onChange={(e) => setTaskFormData({ ...taskFormData, dueDate: e.target.value })}
                />
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              {canManage && (
                <Input
                  label="Estimated Hours"
                  type="number"
                  value={taskFormData.estimatedHours ?? 0}
                  onChange={(e) => setTaskFormData({ ...taskFormData, estimatedHours: Number(e.target.value) })}
                />
              )}
              <Input
                label="Actual Hours Logged"
                type="number"
                value={taskFormData.actualHours ?? 0}
                onChange={(e) => setTaskFormData({ ...taskFormData, actualHours: Number(e.target.value) })}
              />
            </div>

            {canManage && (
              <div>
                <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
                  Description
                </label>
                <textarea
                  rows={3}
                  value={taskFormData.description || ''}
                  onChange={(e) => setTaskFormData({ ...taskFormData, description: e.target.value })}
                  className="w-full rounded-lg border border-neutral-200 bg-white p-2.5 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100"
                />
              </div>
            )}

            <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
              <Button variant="outline" type="button" onClick={() => setIsEditMode(false)}>
                Cancel
              </Button>
              <Button type="submit">Save Changes</Button>
            </div>
          </form>
        ) : null}
      </Modal>
    </div>
  );
};
