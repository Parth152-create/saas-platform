import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  CheckCircle2,
  Clock,
  DollarSign,
  FolderKanban,
  Plus,
  Trash2,
  UserPlus,
  Users,
  AlertTriangle,
  FileText,
  Activity,
  Edit2,
  CheckSquare,
  MessageSquare,
  Send,
  Hash,
} from 'lucide-react';
import { chatApi, type ChatChannel, type ChatMessage } from '../../api/chatApi';
import { wsManager } from '../../collaboration/websocketClient';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardHeader, CardTitle, CardContent } from '../../components/common/Card';
import { Tabs } from '../../components/common/Tabs';
import { EmptyState } from '../../components/common/EmptyState';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { Modal } from '../../components/common/Modal';
import { Input } from '../../components/common/Input';
import { Select } from '../../components/common/Select';
import { formatDate } from '../../utils/formatters';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import {
  projectsApi,
  type ProjectResponse,
  type TaskResponse,
  type ProjectMember,
  type ActivityFeedItem,
  type TaskStatus,
  type TaskPriority,
  type CreateTaskData,
} from '../../api/projectsApi';
import { hrmApi, type EmployeeDto } from '../../api/hrmApi';

export const ProjectDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { hasRole } = useAuth();

  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [activities, setActivities] = useState<ActivityFeedItem[]>([]);
  const [employees, setEmployees] = useState<EmployeeDto[]>([]);

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('overview');

  // Task creation/editing
  const [isCreateTaskOpen, setIsCreateTaskOpen] = useState(false);
  const [isEditTaskOpen, setIsEditTaskOpen] = useState(false);
  const [selectedTask, setSelectedTask] = useState<TaskResponse | null>(null);
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

  // Team member modal
  const [isAddMemberOpen, setIsAddMemberOpen] = useState(false);
  const [selectedMemberId, setSelectedMemberId] = useState('');
  const [memberRole, setMemberRole] = useState('MEMBER');

  // Project Channel Chat state
  const [projectChannel, setProjectChannel] = useState<ChatChannel | null>(null);
  const [channelMessages, setChannelMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [isLoadingChat, setIsLoadingChat] = useState(false);
  const [isSendingChat, setIsSendingChat] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);

  const canManage = hasRole('MANAGER');

  const loadData = useCallback(async () => {
    if (!id) return;
    try {
      setIsLoading(true);
      setError(null);
      const [projData, taskData, memberData, actData] = await Promise.all([
        projectsApi.getProject(id),
        projectsApi.getProjectTasks(id),
        projectsApi.getProjectMembers(id),
        projectsApi.getProjectActivities(id),
      ]);
      setProject(projData);
      setTasks(taskData);
      setMembers(memberData);
      setActivities(actData);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load project details';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!id) return;
    let isMounted = true;
    Promise.all([
      projectsApi.getProject(id),
      projectsApi.getProjectTasks(id),
      projectsApi.getProjectMembers(id),
      projectsApi.getProjectActivities(id),
    ])
      .then(([projData, taskData, memberData, actData]) => {
        if (isMounted) {
          setProject(projData);
          setTasks(taskData);
          setMembers(memberData);
          setActivities(actData);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to load project details';
          setError(msg);
        }
      })
      .finally(() => {
        if (isMounted) {
          setIsLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [id]);

  useEffect(() => {
    hrmApi.getEmployees()
      .then(setEmployees)
      .catch(() => setEmployees([]));
  }, []);

  const handleOpenCreateTask = () => {
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
    setIsCreateTaskOpen(true);
  };

  const handleCreateTaskSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !taskFormData.title.trim()) return;

    try {
      await projectsApi.createTask(id, {
        ...taskFormData,
        assigneeId: taskFormData.assigneeId || undefined,
        estimatedHours: Number(taskFormData.estimatedHours) || 0,
        actualHours: Number(taskFormData.actualHours) || 0,
      });
      showToast('success', 'Task Created', `Task "${taskFormData.title}" created.`);
      setIsCreateTaskOpen(false);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create task';
      showToast('error', 'Failed', msg);
    }
  };

  const handleOpenEditTask = (task: TaskResponse) => {
    setSelectedTask(task);
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
    setIsEditTaskOpen(true);
  };

  const handleEditTaskSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTask) return;

    try {
      await projectsApi.updateTask(selectedTask.id, {
        ...taskFormData,
        assigneeId: taskFormData.assigneeId || undefined,
        estimatedHours: Number(taskFormData.estimatedHours) || 0,
        actualHours: Number(taskFormData.actualHours) || 0,
      });
      showToast('success', 'Task Updated', `Task "${taskFormData.title}" updated.`);
      setIsEditTaskOpen(false);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update task';
      showToast('error', 'Failed', msg);
    }
  };

  const handleQuickStatusChange = async (task: TaskResponse, newStatus: TaskStatus) => {
    try {
      await projectsApi.updateTask(task.id, { status: newStatus });
      showToast('success', 'Status Updated', `Task status changed to ${newStatus}.`);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update status';
      showToast('error', 'Failed', msg);
    }
  };

  const handleDeleteTask = async (taskId: string, title: string) => {
    if (!window.confirm(`Delete task "${title}"?`)) return;
    try {
      await projectsApi.deleteTask(taskId);
      showToast('success', 'Task Deleted', `Task "${title}" removed.`);
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete task';
      showToast('error', 'Failed', msg);
    }
  };

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !selectedMemberId) return;

    try {
      await projectsApi.addProjectMember(id, selectedMemberId, memberRole);
      showToast('success', 'Team Member Added', 'User assigned to project.');
      setIsAddMemberOpen(false);
      setSelectedMemberId('');
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to add member';
      showToast('error', 'Failed', msg);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!id || !window.confirm('Remove this member from the project team?')) return;
    try {
      await projectsApi.removeProjectMember(id, memberId);
      showToast('info', 'Member Removed', 'Team member removed.');
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to remove member';
      showToast('error', 'Failed', msg);
    }
  };

  useEffect(() => {
    if (activeTab === 'chat' && id) {
      chatApi
        .getOrCreateProjectChannel(id)
        .then((chan) => {
          setProjectChannel(chan);
          return chatApi.getChannelMessages(chan.id);
        })
        .then((msgRes) => {
          const sorted = [...(msgRes.content || [])].reverse();
          setChannelMessages(sorted);
          setIsLoadingChat(false);
          setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
        })
        .catch((err) => {
          console.error('Failed to load project channel:', err);
          setIsLoadingChat(false);
        });
    }
  }, [activeTab, id]);

  useEffect(() => {
    if (activeTab === 'chat' && projectChannel) {
      const unsub = wsManager.subscribeToChannelMessage<ChatMessage>(
        projectChannel.id,
        (msg) => {
          setChannelMessages((prev) => {
            if (prev.some((m) => m.id === msg.id)) return prev;
            return [...prev, msg];
          });
          setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
        }
      );
      return () => unsub();
    }
  }, [activeTab, projectChannel]);

  const handleSendProjectMessage = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!chatInput.trim() || !projectChannel || isSendingChat) return;
    const content = chatInput.trim();
    setChatInput('');
    setIsSendingChat(true);
    try {
      const sent = await chatApi.sendChannelMessage(projectChannel.id, content);
      setChannelMessages((prev) => {
        if (prev.some((m) => m.id === sent.id)) return prev;
        return [...prev, sent];
      });
      setTimeout(() => chatEndRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
    } catch (err) {
      console.error('Failed to send project message:', err);
      setChatInput(content);
    } finally {
      setIsSendingChat(false);
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <LoadingSkeleton variant="table" rows={6} />
      </div>
    );
  }

  if (error || !project) {
    return (
      <div className="space-y-6">
        <ErrorAlert
          title="Project not found"
          message={error || 'The requested project could not be found in this workspace.'}
          onRetry={() => navigate('/app/projects')}
        />
      </div>
    );
  }

  const tabs = [
    { id: 'overview', label: 'Overview', icon: <FolderKanban className="w-4 h-4" /> },
    { id: 'tasks', label: `Tasks (${tasks.length})`, icon: <CheckSquare className="w-4 h-4" /> },
    { id: 'team', label: `Team (${members.length})`, icon: <Users className="w-4 h-4" /> },
    { id: 'chat', label: 'Chat', icon: <MessageSquare className="w-4 h-4" /> },
    { id: 'activity', label: 'Activity', icon: <Activity className="w-4 h-4" /> },
    { id: 'details', label: 'Details', icon: <FileText className="w-4 h-4" /> },
  ];

  return (
    <div className="space-y-6">
      {/* Header with Back Button */}
      <div className="flex items-center gap-3">
        <Button
          variant="outline"
          size="sm"
          onClick={() => navigate('/app/projects')}
          leftIcon={<ArrowLeft className="w-3.5 h-3.5" />}
        >
          Back to Projects
        </Button>
      </div>

      <PageHeader
        title={project.name}
        description={project.client ? `Client: ${project.client}` : 'Internal Workspace Project'}
        actions={
          <div className="flex items-center gap-2">
            <Badge variant="primary" size="sm">
              {project.priority} PRIORITY
            </Badge>
            <Badge variant={project.status === 'COMPLETED' ? 'success' : 'default'} size="sm" withDot>
              {project.status}
            </Badge>
          </div>
        }
      />

      {/* Tabs Navigation */}
      <Tabs tabs={tabs} activeTab={activeTab} onChange={setActiveTab} />

      {/* Tab: Overview */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
          {/* Key Metrics Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            <Card>
              <CardContent className="p-4">
                <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
                  Progress
                </span>
                <div className="mt-2 flex items-center justify-between">
                  <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                    {project.progress}%
                  </span>
                  <div className="w-16 bg-neutral-100 dark:bg-[#202020] h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-neutral-900 dark:bg-white h-full rounded-full transition-all duration-300"
                      style={{ width: `${project.progress}%` }}
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
                  Tasks
                </span>
                <div className="mt-2 flex items-center justify-between">
                  <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                    {project.doneTasks} / {project.totalTasks}
                  </span>
                  <span className="text-xs text-neutral-500 font-medium">Completed</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
                  Overdue Tasks
                </span>
                <div className="mt-2 flex items-center justify-between">
                  <span className={`text-2xl font-bold ${project.overdueTasks > 0 ? 'text-rose-600 dark:text-rose-400' : 'text-neutral-900 dark:text-neutral-100'}`}>
                    {project.overdueTasks}
                  </span>
                  {project.overdueTasks > 0 ? (
                    <Badge variant="danger" size="sm">Action Needed</Badge>
                  ) : (
                    <Badge variant="success" size="sm">On Schedule</Badge>
                  )}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
                  Team
                </span>
                <div className="mt-2 flex items-center justify-between">
                  <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                    {members.length}
                  </span>
                  <span className="text-xs text-neutral-500 font-medium">Members</span>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-4">
                <span className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wider block">
                  Budget
                </span>
                <div className="mt-2 flex items-center justify-between">
                  <span className="text-2xl font-bold font-mono text-neutral-900 dark:text-neutral-100">
                    {project.budget ? `$${Number(project.budget).toLocaleString()}` : '—'}
                  </span>
                  <DollarSign className="w-4 h-4 text-neutral-400" />
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Project Details Section */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle className="text-sm font-semibold">Description & Objectives</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-xs text-neutral-600 dark:text-neutral-400 whitespace-pre-wrap leading-relaxed">
                  {project.description || 'No detailed description provided for this project.'}
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-semibold">Project Metadata</CardTitle>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex items-center justify-between text-xs py-1 border-b border-neutral-100 dark:border-[#202020]">
                  <span className="text-neutral-400">Project Lead / Owner</span>
                  <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                    {project.ownerName || 'Unassigned'}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs py-1 border-b border-neutral-100 dark:border-[#202020]">
                  <span className="text-neutral-400">Client Organization</span>
                  <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                    {project.client || 'Internal'}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs py-1 border-b border-neutral-100 dark:border-[#202020]">
                  <span className="text-neutral-400">Start Date</span>
                  <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                    {formatDate(project.startDate)}
                  </span>
                </div>
                <div className="flex items-center justify-between text-xs py-1">
                  <span className="text-neutral-400">Due Date</span>
                  <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                    {formatDate(project.dueDate)}
                  </span>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

      {/* Tab: Tasks */}
      {activeTab === 'tasks' && (
        <Card>
          <CardHeader className="flex items-center justify-between gap-4 flex-wrap">
            <div>
              <CardTitle className="text-sm font-semibold">Deliverables & Tasks</CardTitle>
              <p className="text-xs text-neutral-400 mt-0.5">
                Tasks associated with this project and assigned to workspace members.
              </p>
            </div>
            {canManage && (
              <Button
                size="sm"
                onClick={handleOpenCreateTask}
                leftIcon={<Plus className="w-3.5 h-3.5" />}
              >
                Add Task
              </Button>
            )}
          </CardHeader>
          <CardContent className="p-0">
            {tasks.length === 0 ? (
              <div className="p-8 text-center">
                <EmptyState
                  icon={<CheckSquare className="w-6 h-6" />}
                  title="No tasks in this project"
                  description="Create the first operational task or milestone for this project."
                  actionLabel={canManage ? 'Add Task' : undefined}
                  onAction={canManage ? handleOpenCreateTask : undefined}
                  className="border-0 rounded-none bg-transparent"
                />
              </div>
            ) : (
              <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {tasks.map((task) => {
                  const isDone = task.status === 'DONE';
                  const isOverdue = task.dueDate && new Date(task.dueDate) < new Date() && !isDone;

                  return (
                    <div
                      key={task.id}
                      className="p-4 flex items-center justify-between gap-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                    >
                      <div className="flex items-start gap-3 min-w-0 flex-1">
                        <input
                          type="checkbox"
                          checked={isDone}
                          onChange={() => handleQuickStatusChange(task, isDone ? 'TODO' : 'DONE')}
                          className="mt-1 h-4 w-4 rounded border-neutral-300 dark:border-[#262626] text-neutral-900 focus:ring-neutral-500 cursor-pointer accent-neutral-900 dark:accent-neutral-100"
                        />
                        <div className="min-w-0">
                          <p className={`text-xs font-semibold ${isDone ? 'line-through text-neutral-400 dark:text-neutral-500' : 'text-neutral-900 dark:text-neutral-100'}`}>
                            {task.title}
                          </p>
                          {task.description && (
                            <p className="text-[11px] text-neutral-500 line-clamp-1 mt-0.5">
                              {task.description}
                            </p>
                          )}
                          <div className="flex items-center gap-3 text-[10px] text-neutral-400 mt-1 flex-wrap">
                            <span>Assignee: <strong className="text-neutral-700 dark:text-neutral-300">{task.assigneeName || 'Unassigned'}</strong></span>
                            <span>•</span>
                            <span className={isOverdue ? 'text-rose-500 font-semibold' : ''}>
                              Due {formatDate(task.dueDate)}
                            </span>
                            {task.estimatedHours > 0 && (
                              <>
                                <span>•</span>
                                <span>{task.actualHours} / {task.estimatedHours} hrs</span>
                              </>
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <Badge
                          variant={task.priority === 'URGENT' || task.priority === 'HIGH' ? 'danger' : task.priority === 'MEDIUM' ? 'warning' : 'default'}
                          size="sm"
                        >
                          {task.priority}
                        </Badge>
                        <Badge
                          variant={isDone ? 'success' : task.status === 'IN_PROGRESS' ? 'primary' : 'default'}
                          size="sm"
                        >
                          {task.status}
                        </Badge>

                        {canManage && (
                          <div className="flex items-center gap-1 ml-2">
                            <button
                              type="button"
                              onClick={() => handleOpenEditTask(task)}
                              className="p-1 rounded text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200"
                            >
                              <Edit2 className="w-3.5 h-3.5" />
                            </button>
                            <button
                              type="button"
                              onClick={() => handleDeleteTask(task.id, task.title)}
                              className="p-1 rounded text-neutral-400 hover:text-rose-600"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab: Team */}
      {activeTab === 'team' && (
        <Card>
          <CardHeader className="flex items-center justify-between gap-4 flex-wrap">
            <div>
              <CardTitle className="text-sm font-semibold">Assigned Team Members</CardTitle>
              <p className="text-xs text-neutral-400 mt-0.5">
                Staff members authorized to execute deliverables in this project.
              </p>
            </div>
            {canManage && (
              <Button
                size="sm"
                onClick={() => setIsAddMemberOpen(true)}
                leftIcon={<UserPlus className="w-3.5 h-3.5" />}
              >
                Assign Member
              </Button>
            )}
          </CardHeader>
          <CardContent className="p-0">
            {members.length === 0 ? (
              <div className="p-8 text-center">
                <EmptyState
                  icon={<Users className="w-6 h-6" />}
                  title="No team members assigned"
                  description="Assign colleagues to collaborate on this project."
                  actionLabel={canManage ? 'Assign Member' : undefined}
                  onAction={canManage ? () => setIsAddMemberOpen(true) : undefined}
                  className="border-0 rounded-none bg-transparent"
                />
              </div>
            ) : (
              <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {members.map((m) => (
                  <div key={m.id} className="p-4 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="h-8 w-8 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center font-bold text-xs text-neutral-800 dark:text-neutral-200">
                        {m.memberName ? m.memberName.charAt(0).toUpperCase() : 'U'}
                      </div>
                      <div>
                        <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100">
                          {m.memberName}
                        </p>
                        <p className="text-[11px] text-neutral-400">{m.memberEmail}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <Badge variant="default" size="sm">
                        {m.role}
                      </Badge>
                      {canManage && (
                        <button
                          type="button"
                          onClick={() => handleRemoveMember(m.memberId)}
                          className="p-1 rounded text-neutral-400 hover:text-rose-600 transition-colors"
                          title="Remove from project"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab: Chat */}
      {activeTab === 'chat' && (
        <Card className="h-[550px] flex flex-col overflow-hidden">
          <CardHeader className="py-3 px-4 border-b border-neutral-200 dark:border-[#262626] flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Hash className="w-4 h-4 text-blue-500" />
              <CardTitle className="text-sm font-semibold">
                #{projectChannel?.name || `project-${project.name.toLowerCase().replace(/\s+/g, '-')}`}
              </CardTitle>
              <Badge variant="default" size="sm">
                Project Channel
              </Badge>
            </div>
            <span className="text-xs text-neutral-400">
              {members.length} team members
            </span>
          </CardHeader>

          <CardContent className="flex-1 overflow-y-auto p-4 space-y-3">
            {isLoadingChat ? (
              <div className="space-y-3 p-2">
                <LoadingSkeleton variant="text" rows={4} />
              </div>
            ) : channelMessages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-8 text-neutral-400">
                <MessageSquare className="w-10 h-10 mb-2 opacity-30" />
                <p className="text-sm font-medium">No messages yet in this project channel</p>
                <p className="text-xs mt-1">Start collaborating with your project team below.</p>
              </div>
            ) : (
              channelMessages.map((msg) => (
                <div key={msg.id} className="flex gap-2.5 text-xs">
                  <div className="w-7 h-7 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center font-bold text-[11px] shrink-0 text-neutral-700 dark:text-neutral-200">
                    {msg.senderName?.charAt(0).toUpperCase() || 'U'}
                  </div>
                  <div className="min-w-0 flex-1 bg-neutral-50 dark:bg-[#181818] p-2.5 rounded-xl border border-neutral-100 dark:border-[#222]">
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-semibold text-neutral-800 dark:text-neutral-200">
                        {msg.senderName}
                      </span>
                      <span className="text-[10px] text-neutral-400">
                        {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-neutral-700 dark:text-neutral-300 leading-relaxed whitespace-pre-wrap break-words">
                      {msg.content}
                    </p>
                  </div>
                </div>
              ))
            )}
            <div ref={chatEndRef} />
          </CardContent>

          <div className="p-3 border-t border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#121212]">
            <form onSubmit={handleSendProjectMessage} className="flex items-center gap-2">
              <Input
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                placeholder={`Message #${projectChannel?.name || 'project-channel'}...`}
                className="text-xs"
                disabled={isSendingChat}
              />
              <Button
                type="submit"
                size="sm"
                disabled={!chatInput.trim() || isSendingChat}
                leftIcon={<Send className="w-3.5 h-3.5" />}
              >
                Send
              </Button>
            </form>
          </div>
        </Card>
      )}

      {/* Tab: Activity */}
      {activeTab === 'activity' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">Project Audit &amp; Event Stream</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {activities.length === 0 ? (
              <div className="p-8 text-center">
                <EmptyState
                  icon={<Activity className="w-6 h-6" />}
                  title="No recorded activities yet"
                  description="Actions taken on this project and its tasks will appear here in real time."
                  className="border-0 rounded-none bg-transparent"
                />
              </div>
            ) : (
              <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {activities.map((act) => (
                  <div key={act.id} className="p-4 flex items-start gap-3">
                    <div className="mt-0.5 flex h-7 w-7 items-center justify-center rounded-full bg-neutral-100 text-neutral-900 dark:bg-[#1f1f1f] dark:text-neutral-100 shrink-0">
                      {act.type === 'success' ? (
                        <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                      ) : act.type === 'warning' ? (
                        <AlertTriangle className="w-4 h-4 text-amber-500" />
                      ) : (
                        <Clock className="w-4 h-4 text-neutral-500" />
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs text-neutral-700 dark:text-neutral-300">
                        <strong className="font-semibold text-neutral-900 dark:text-neutral-100">
                          {act.user}
                        </strong>{' '}
                        {act.action}{' '}
                        <span className="font-medium text-neutral-900 dark:text-neutral-100">
                          &quot;{act.target}&quot;
                        </span>
                      </p>
                      <span className="text-[10px] text-neutral-400 mt-1 block">
                        {act.timestamp}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab: Details */}
      {activeTab === 'details' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-sm font-semibold">Technical &amp; Record Details</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div>
                <label className="text-neutral-400 block mb-1">Project Identifier (UUID)</label>
                <div className="font-mono bg-neutral-50 dark:bg-[#141414] p-2 rounded border border-neutral-100 dark:border-[#222]">
                  {project.id}
                </div>
              </div>
              <div>
                <label className="text-neutral-400 block mb-1">Created At</label>
                <div className="font-mono bg-neutral-50 dark:bg-[#141414] p-2 rounded border border-neutral-100 dark:border-[#222]">
                  {project.createdAt}
                </div>
              </div>
              <div>
                <label className="text-neutral-400 block mb-1">Last Updated</label>
                <div className="font-mono bg-neutral-50 dark:bg-[#141414] p-2 rounded border border-neutral-100 dark:border-[#222]">
                  {project.updatedAt}
                </div>
              </div>
              <div>
                <label className="text-neutral-400 block mb-1">Assigned Members Count</label>
                <div className="font-mono bg-neutral-50 dark:bg-[#141414] p-2 rounded border border-neutral-100 dark:border-[#222]">
                  {members.length} team members
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Create Task Modal */}
      <Modal
        isOpen={isCreateTaskOpen}
        onClose={() => setIsCreateTaskOpen(false)}
        title="Add New Project Task"
        size="md"
      >
        <form onSubmit={handleCreateTaskSubmit} className="space-y-4">
          <Input
            label="Task Title *"
            value={taskFormData.title}
            onChange={(e) => setTaskFormData({ ...taskFormData, title: e.target.value })}
            placeholder="e.g. Implement schema migration"
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
              Description
            </label>
            <textarea
              rows={3}
              value={taskFormData.description || ''}
              onChange={(e) => setTaskFormData({ ...taskFormData, description: e.target.value })}
              placeholder="Task details..."
              className="w-full rounded-lg border border-neutral-200 bg-white p-2 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="outline" type="button" onClick={() => setIsCreateTaskOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">Create Task</Button>
          </div>
        </form>
      </Modal>

      {/* Edit Task Modal */}
      <Modal
        isOpen={isEditTaskOpen}
        onClose={() => setIsEditTaskOpen(false)}
        title="Edit Task Details"
        size="md"
      >
        <form onSubmit={handleEditTaskSubmit} className="space-y-4">
          <Input
            label="Task Title *"
            value={taskFormData.title}
            onChange={(e) => setTaskFormData({ ...taskFormData, title: e.target.value })}
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
              Description
            </label>
            <textarea
              rows={3}
              value={taskFormData.description || ''}
              onChange={(e) => setTaskFormData({ ...taskFormData, description: e.target.value })}
              className="w-full rounded-lg border border-neutral-200 bg-white p-2 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="outline" type="button" onClick={() => setIsEditTaskOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">Save Changes</Button>
          </div>
        </form>
      </Modal>

      {/* Add Member Modal */}
      <Modal
        isOpen={isAddMemberOpen}
        onClose={() => setIsAddMemberOpen(false)}
        title="Assign Team Member to Project"
        size="sm"
      >
        <form onSubmit={handleAddMember} className="space-y-4">
          <Select
            label="Select Colleague *"
            value={selectedMemberId}
            onChange={(e) => setSelectedMemberId(e.target.value)}
            options={[
              { value: '', label: 'Select a colleague...' },
              ...employees.map((emp) => ({
                value: emp.id,
                label: `${emp.name} — ${emp.department}`,
              })),
            ]}
            required
          />

          <Select
            label="Role in Project"
            value={memberRole}
            onChange={(e) => setMemberRole(e.target.value)}
            options={[
              { value: 'MEMBER', label: 'Team Member' },
              { value: 'LEAD', label: 'Technical Lead' },
              { value: 'REVIEWER', label: 'Reviewer' },
              { value: 'STAKEHOLDER', label: 'Stakeholder' },
            ]}
          />

          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="outline" type="button" onClick={() => setIsAddMemberOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">Add to Project</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
