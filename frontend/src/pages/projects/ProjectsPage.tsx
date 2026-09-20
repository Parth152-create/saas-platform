import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FolderKanban,
  Plus,
  Search,
  Archive,
  Trash2,
  Edit2,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent } from '../../components/common/Card';
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
  type ProjectStatus,
  type ProjectPriority,
  type CreateProjectData,
} from '../../api/projectsApi';
import { hrmApi, type EmployeeDto } from '../../api/hrmApi';

export const ProjectsPage: React.FC = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { hasRole } = useAuth();

  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [employees, setEmployees] = useState<EmployeeDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<string>('ALL');

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [selectedProject, setSelectedProject] = useState<ProjectResponse | null>(null);
  const [isDeleting, setIsDeleting] = useState<string | null>(null);

  // Form State
  const [formData, setFormData] = useState<CreateProjectData>({
    name: '',
    description: '',
    client: '',
    status: 'PLANNING',
    priority: 'MEDIUM',
    startDate: '',
    dueDate: '',
    budget: undefined,
    ownerId: '',
    memberIds: [],
  });

  const canManage = hasRole('MANAGER');
  const canAdmin = hasRole('ADMIN');

  const fetchProjects = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await projectsApi.getProjects({
        status: statusFilter !== 'ALL' ? statusFilter : undefined,
        priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
        search: search.trim() || undefined,
      });
      setProjects(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load projects';
      setError(msg);
      showToast('error', 'Error Loading Projects', msg);
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter, priorityFilter, search, showToast]);

  useEffect(() => {
    let isMounted = true;
    projectsApi.getProjects({
      status: statusFilter !== 'ALL' ? statusFilter : undefined,
      priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
      search: search.trim() || undefined,
    })
      .then((data) => {
        if (isMounted) setProjects(data);
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to load projects';
          setError(msg);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [statusFilter, priorityFilter, search]);

  useEffect(() => {
    hrmApi.getEmployees()
      .then(setEmployees)
      .catch(() => setEmployees([]));
  }, []);

  const handleOpenCreate = () => {
    setFormData({
      name: '',
      description: '',
      client: '',
      status: 'PLANNING',
      priority: 'MEDIUM',
      startDate: new Date().toISOString().split('T')[0],
      dueDate: '',
      budget: undefined,
      ownerId: '',
      memberIds: [],
    });
    setIsCreateOpen(true);
  };

  const handleOpenEdit = (project: ProjectResponse, e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedProject(project);
    setFormData({
      name: project.name,
      description: project.description || '',
      client: project.client || '',
      status: project.status,
      priority: project.priority,
      startDate: project.startDate || '',
      dueDate: project.dueDate || '',
      budget: project.budget ?? undefined,
      ownerId: project.ownerId || '',
      memberIds: project.members?.map((m) => m.memberId) || [],
    });
    setIsEditOpen(true);
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) return;

    try {
      await projectsApi.createProject({
        ...formData,
        budget: formData.budget ? Number(formData.budget) : undefined,
        ownerId: formData.ownerId || undefined,
        memberIds: formData.memberIds && formData.memberIds.length > 0 ? formData.memberIds : undefined,
      });
      showToast('success', 'Project Created', `Project "${formData.name}" has been created.`);
      setIsCreateOpen(false);
      fetchProjects();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to create project';
      showToast('error', 'Creation Failed', msg);
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedProject || !formData.name.trim()) return;

    try {
      await projectsApi.updateProject(selectedProject.id, {
        ...formData,
        budget: formData.budget !== undefined ? Number(formData.budget) : undefined,
        ownerId: formData.ownerId || undefined,
        memberIds: formData.memberIds,
      });
      showToast('success', 'Project Updated', `Project "${formData.name}" updated successfully.`);
      setIsEditOpen(false);
      fetchProjects();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update project';
      showToast('error', 'Update Failed', msg);
    }
  };

  const handleDelete = async (id: string, name: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (!window.confirm(`Are you sure you want to permanently delete project "${name}" and all its tasks?`)) {
      return;
    }

    try {
      setIsDeleting(id);
      await projectsApi.deleteProject(id);
      showToast('success', 'Project Deleted', `Project "${name}" was deleted.`);
      fetchProjects();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to delete project';
      showToast('error', 'Delete Failed', msg);
    } finally {
      setIsDeleting(null);
    }
  };

  const handleArchive = async (project: ProjectResponse, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await projectsApi.updateProject(project.id, { status: 'ARCHIVED' });
      showToast('info', 'Project Archived', `Project "${project.name}" has been archived.`);
      fetchProjects();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to archive project';
      showToast('error', 'Archive Failed', msg);
    }
  };

  const getStatusVariant = (status: ProjectStatus) => {
    switch (status) {
      case 'ACTIVE':
        return 'primary';
      case 'COMPLETED':
        return 'success';
      case 'ON_HOLD':
        return 'warning';
      case 'ARCHIVED':
        return 'default';
      default:
        return 'default';
    }
  };

  const getPriorityVariant = (priority: ProjectPriority) => {
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
        title="Enterprise Projects"
        description="Active customer implementations, internal infrastructure sprints, and project milestones."
        actions={
          canManage && (
            <Button
              size="sm"
              onClick={handleOpenCreate}
              leftIcon={<Plus className="w-4 h-4" />}
            >
              New Project
            </Button>
          )
        }
      />

      {error && (
        <ErrorAlert
          title="Error loading projects"
          message={error}
          onRetry={fetchProjects}
        />
      )}

      <Card>
        {/* Filter / Search Bar */}
        <div className="p-4 border-b border-neutral-100 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="relative w-full md:max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-neutral-400" />
            <input
              type="text"
              placeholder="Search projects by title, client, or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full rounded-lg border border-neutral-200 bg-white pl-9 pr-4 py-2 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500 dark:focus:border-white"
            />
          </div>

          <div className="flex items-center gap-3 w-full md:w-auto">
            <Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              options={[
                { value: 'ALL', label: 'All Statuses' },
                { value: 'PLANNING', label: 'Planning' },
                { value: 'ACTIVE', label: 'Active' },
                { value: 'ON_HOLD', label: 'On Hold' },
                { value: 'COMPLETED', label: 'Completed' },
                { value: 'ARCHIVED', label: 'Archived' },
              ]}
              className="w-full md:w-36 text-xs"
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

        <CardContent className="p-0">
          {isLoading ? (
            <div className="p-6">
              <LoadingSkeleton variant="table" rows={5} />
            </div>
          ) : projects.length === 0 ? (
            <div className="p-12 text-center">
              <EmptyState
                icon={<FolderKanban className="w-8 h-8" />}
                title={search || statusFilter !== 'ALL' || priorityFilter !== 'ALL' ? 'No projects match your criteria' : 'No active projects'}
                description={
                  search || statusFilter !== 'ALL' || priorityFilter !== 'ALL'
                    ? 'Try clearing filters or search terms to find what you are looking for.'
                    : 'Create your first project to start tracking deliverables, tasks, and team milestones.'
                }
                actionLabel={
                  search || statusFilter !== 'ALL' || priorityFilter !== 'ALL'
                    ? 'Clear Filters'
                    : canManage
                    ? 'New Project'
                    : undefined
                }
                onAction={
                  search || statusFilter !== 'ALL' || priorityFilter !== 'ALL'
                    ? () => {
                        setSearch('');
                        setStatusFilter('ALL');
                        setPriorityFilter('ALL');
                      }
                    : canManage
                    ? handleOpenCreate
                    : undefined
                }
                className="border-0 rounded-none bg-transparent"
              />
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                  <tr>
                    <th className="px-5 py-3">Project</th>
                    <th className="px-5 py-3">Client</th>
                    <th className="px-5 py-3">Priority</th>
                    <th className="px-5 py-3">Progress</th>
                    <th className="px-5 py-3">Tasks</th>
                    <th className="px-5 py-3">Due Date</th>
                    <th className="px-5 py-3">Budget</th>
                    <th className="px-5 py-3">Status</th>
                    <th className="px-5 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                  {projects.map((proj) => {
                    const isOverdue = proj.dueDate && new Date(proj.dueDate) < new Date() && proj.status !== 'COMPLETED';

                    return (
                      <tr
                        key={proj.id}
                        onClick={() => navigate(`/app/projects/${proj.id}`)}
                        className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors cursor-pointer"
                      >
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-2.5">
                            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-neutral-100 dark:bg-[#202020] text-neutral-900 dark:text-neutral-100 shrink-0">
                              <FolderKanban className="w-4 h-4" />
                            </div>
                            <div className="min-w-0">
                              <p className="font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                                {proj.name}
                              </p>
                              {proj.ownerName && (
                                <p className="text-[10px] text-neutral-400 truncate">
                                  Owner: {proj.ownerName}
                                </p>
                              )}
                            </div>
                          </div>
                        </td>

                        <td className="px-5 py-3.5 font-medium text-neutral-700 dark:text-neutral-300">
                          {proj.client || '—'}
                        </td>

                        <td className="px-5 py-3.5">
                          <Badge variant={getPriorityVariant(proj.priority)} size="sm">
                            {proj.priority}
                          </Badge>
                        </td>

                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-2">
                            <div className="w-20 bg-neutral-100 dark:bg-[#1f1f1f] h-2 rounded-full overflow-hidden">
                              <div
                                className="bg-neutral-950 dark:bg-white h-full rounded-full transition-all duration-300"
                                style={{ width: `${proj.progress}%` }}
                              />
                            </div>
                            <span className="font-semibold text-[11px] text-neutral-800 dark:text-neutral-200">
                              {proj.progress}%
                            </span>
                          </div>
                        </td>

                        <td className="px-5 py-3.5">
                          <span className="text-neutral-800 dark:text-neutral-200 font-medium">
                            {proj.doneTasks} / {proj.totalTasks}
                          </span>
                          {proj.overdueTasks > 0 && (
                            <span className="ml-1.5 text-[10px] text-rose-500 font-semibold">
                              ({proj.overdueTasks} overdue)
                            </span>
                          )}
                        </td>

                        <td className="px-5 py-3.5">
                          <span className={isOverdue ? 'text-rose-600 dark:text-rose-400 font-semibold flex items-center gap-1' : ''}>
                            {formatDate(proj.dueDate)}
                          </span>
                        </td>

                        <td className="px-5 py-3.5 font-mono font-medium text-neutral-900 dark:text-neutral-100">
                          {proj.budget ? `$${Number(proj.budget).toLocaleString()}` : '—'}
                        </td>

                        <td className="px-5 py-3.5">
                          <Badge variant={getStatusVariant(proj.status)} size="sm" withDot>
                            {proj.status}
                          </Badge>
                        </td>

                        <td className="px-5 py-3.5 text-right">
                          <div className="flex items-center justify-end gap-1.5" onClick={(e) => e.stopPropagation()}>
                            {canManage && (
                              <button
                                type="button"
                                onClick={(e) => handleOpenEdit(proj, e)}
                                className="p-1.5 rounded text-neutral-400 hover:text-neutral-800 dark:hover:text-neutral-100 hover:bg-neutral-100 dark:hover:bg-[#222]"
                                title="Edit project"
                              >
                                <Edit2 className="w-3.5 h-3.5" />
                              </button>
                            )}

                            {canManage && proj.status !== 'ARCHIVED' && (
                              <button
                                type="button"
                                onClick={(e) => handleArchive(proj, e)}
                                className="p-1.5 rounded text-neutral-400 hover:text-neutral-800 dark:hover:text-neutral-100 hover:bg-neutral-100 dark:hover:bg-[#222]"
                                title="Archive project"
                              >
                                <Archive className="w-3.5 h-3.5" />
                              </button>
                            )}

                            {canAdmin && (
                              <button
                                type="button"
                                disabled={isDeleting === proj.id}
                                onClick={(e) => handleDelete(proj.id, proj.name, e)}
                                className="p-1.5 rounded text-neutral-400 hover:text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/30"
                                title="Delete project"
                              >
                                <Trash2 className="w-3.5 h-3.5" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Create Project Modal */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Create New Project"
        size="lg"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-4">
          <Input
            label="Project Name *"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            placeholder="e.g. Enterprise Migration Q4"
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Client / Department"
              value={formData.client || ''}
              onChange={(e) => setFormData({ ...formData, client: e.target.value })}
              placeholder="e.g. Acme Corp or Internal Engineering"
            />
            <Input
              label="Budget ($)"
              type="number"
              step="0.01"
              value={formData.budget !== undefined ? formData.budget : ''}
              onChange={(e) => setFormData({ ...formData, budget: e.target.value ? Number(e.target.value) : undefined })}
              placeholder="e.g. 50000"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Status"
              value={formData.status || 'PLANNING'}
              onChange={(e) => setFormData({ ...formData, status: e.target.value as ProjectStatus })}
              options={[
                { value: 'PLANNING', label: 'Planning' },
                { value: 'ACTIVE', label: 'Active' },
                { value: 'ON_HOLD', label: 'On Hold' },
                { value: 'COMPLETED', label: 'Completed' },
              ]}
            />
            <Select
              label="Priority"
              value={formData.priority || 'MEDIUM'}
              onChange={(e) => setFormData({ ...formData, priority: e.target.value as ProjectPriority })}
              options={[
                { value: 'LOW', label: 'Low' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'HIGH', label: 'High' },
                { value: 'URGENT', label: 'Urgent' },
              ]}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Start Date"
              type="date"
              value={formData.startDate || ''}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
            />
            <Input
              label="Due Date"
              type="date"
              value={formData.dueDate || ''}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
            />
          </div>

          <Select
            label="Project Owner"
            value={formData.ownerId || ''}
            onChange={(e) => setFormData({ ...formData, ownerId: e.target.value })}
            options={[
              { value: '', label: 'Unassigned (or current user)' },
              ...employees.map((emp) => ({
                value: emp.id,
                label: `${emp.name} (${emp.position})`,
              })),
            ]}
          />

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Description
            </label>
            <textarea
              rows={3}
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Outline project objectives, key milestones, and deliverables..."
              className="w-full rounded-lg border border-neutral-200 bg-white p-2.5 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="outline" type="button" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">
              Create Project
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit Project Modal */}
      <Modal
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        title="Edit Project Details"
        size="lg"
      >
        <form onSubmit={handleEditSubmit} className="space-y-4">
          <Input
            label="Project Name *"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Client / Department"
              value={formData.client || ''}
              onChange={(e) => setFormData({ ...formData, client: e.target.value })}
            />
            <Input
              label="Budget ($)"
              type="number"
              step="0.01"
              value={formData.budget !== undefined ? formData.budget : ''}
              onChange={(e) => setFormData({ ...formData, budget: e.target.value ? Number(e.target.value) : undefined })}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Status"
              value={formData.status || 'PLANNING'}
              onChange={(e) => setFormData({ ...formData, status: e.target.value as ProjectStatus })}
              options={[
                { value: 'PLANNING', label: 'Planning' },
                { value: 'ACTIVE', label: 'Active' },
                { value: 'ON_HOLD', label: 'On Hold' },
                { value: 'COMPLETED', label: 'Completed' },
                { value: 'ARCHIVED', label: 'Archived' },
              ]}
            />
            <Select
              label="Priority"
              value={formData.priority || 'MEDIUM'}
              onChange={(e) => setFormData({ ...formData, priority: e.target.value as ProjectPriority })}
              options={[
                { value: 'LOW', label: 'Low' },
                { value: 'MEDIUM', label: 'Medium' },
                { value: 'HIGH', label: 'High' },
                { value: 'URGENT', label: 'Urgent' },
              ]}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Start Date"
              type="date"
              value={formData.startDate || ''}
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
            />
            <Input
              label="Due Date"
              type="date"
              value={formData.dueDate || ''}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
            />
          </div>

          <Select
            label="Project Owner"
            value={formData.ownerId || ''}
            onChange={(e) => setFormData({ ...formData, ownerId: e.target.value })}
            options={[
              { value: '', label: 'Unassigned' },
              ...employees.map((emp) => ({
                value: emp.id,
                label: `${emp.name} (${emp.position})`,
              })),
            ]}
          />

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 mb-1">
              Description
            </label>
            <textarea
              rows={3}
              value={formData.description || ''}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full rounded-lg border border-neutral-200 bg-white p-2.5 text-xs text-neutral-900 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900 dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 dark:placeholder-neutral-500"
            />
          </div>

          <div className="flex justify-end gap-3 pt-3 border-t border-neutral-100 dark:border-[#262626]">
            <Button variant="outline" type="button" onClick={() => setIsEditOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">
              Save Changes
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
