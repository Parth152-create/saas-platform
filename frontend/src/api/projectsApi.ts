import { apiClient } from './apiClient';

export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED';
export type ProjectPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface ProjectMember {
  id: string;
  memberId: string;
  memberName: string;
  memberEmail: string;
  role: string;
  joinedAt: string;
}

export interface ProjectResponse {
  id: string;
  name: string;
  description: string | null;
  client: string | null;
  status: ProjectStatus;
  priority: ProjectPriority;
  startDate: string | null;
  dueDate: string | null;
  budget: number | null;
  ownerId: string | null;
  ownerName: string | null;
  progress: number;
  totalTasks: number;
  doneTasks: number;
  overdueTasks: number;
  teamMemberCount: number;
  members: ProjectMember[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectData {
  name: string;
  description?: string;
  client?: string;
  status?: ProjectStatus;
  priority?: ProjectPriority;
  startDate?: string;
  dueDate?: string;
  budget?: number;
  ownerId?: string;
  memberIds?: string[];
}

export interface UpdateProjectData {
  name?: string;
  description?: string;
  client?: string;
  status?: ProjectStatus;
  priority?: ProjectPriority;
  startDate?: string;
  dueDate?: string;
  budget?: number;
  ownerId?: string;
  memberIds?: string[];
}

export interface TaskResponse {
  id: string;
  projectId: string;
  projectName: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId: string | null;
  assigneeName: string | null;
  assigneeEmail: string | null;
  dueDate: string | null;
  estimatedHours: number;
  actualHours: number;
  createdById: string | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskData {
  title: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: string;
  dueDate?: string;
  estimatedHours?: number;
  actualHours?: number;
}

export interface UpdateTaskData {
  title?: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: string;
  dueDate?: string;
  estimatedHours?: number;
  actualHours?: number;
}

export interface ProjectStats {
  activeProjects: number;
  totalProjects: number;
  openTasks: number;
  overdueTasks: number;
  completedTasks: number;
  myTasks: number;
}

export interface ActivityFeedItem {
  id: string;
  user: string;
  action: string;
  target: string;
  timestamp: string;
  type: 'info' | 'success' | 'warning';
  createdAt: string;
}

export const projectsApi = {
  getProjects: (params?: { status?: string; priority?: string; ownerId?: string; search?: string }) => {
    const query = new URLSearchParams();
    if (params?.status) query.append('status', params.status);
    if (params?.priority) query.append('priority', params.priority);
    if (params?.ownerId) query.append('ownerId', params.ownerId);
    if (params?.search) query.append('search', params.search);
    const queryString = query.toString();
    return apiClient.get<ProjectResponse[]>(`/api/projects${queryString ? `?${queryString}` : ''}`);
  },

  getProject: (id: string) =>
    apiClient.get<ProjectResponse>(`/api/projects/${id}`),

  createProject: (data: CreateProjectData) =>
    apiClient.post<ProjectResponse>('/api/projects', data),

  updateProject: (id: string, data: UpdateProjectData) =>
    apiClient.patch<ProjectResponse>(`/api/projects/${id}`, data),

  deleteProject: (id: string) =>
    apiClient.delete<void>(`/api/projects/${id}`),

  getProjectMembers: (projectId: string) =>
    apiClient.get<ProjectMember[]>(`/api/projects/${projectId}/members`),

  addProjectMember: (projectId: string, memberId: string, role = 'MEMBER') =>
    apiClient.post<ProjectMember>(`/api/projects/${projectId}/members`, { memberId, role }),

  removeProjectMember: (projectId: string, memberId: string) =>
    apiClient.delete<void>(`/api/projects/${projectId}/members/${memberId}`),

  getProjectStats: () =>
    apiClient.get<ProjectStats>('/api/projects/stats'),

  getGlobalActivities: () =>
    apiClient.get<ActivityFeedItem[]>('/api/projects/activity'),

  getProjectActivities: (projectId: string) =>
    apiClient.get<ActivityFeedItem[]>(`/api/projects/${projectId}/activity`),

  getProjectTasks: (projectId: string, params?: { status?: string; priority?: string; assigneeId?: string; overdue?: boolean; search?: string }) => {
    const query = new URLSearchParams();
    if (params?.status) query.append('status', params.status);
    if (params?.priority) query.append('priority', params.priority);
    if (params?.assigneeId) query.append('assigneeId', params.assigneeId);
    if (params?.overdue !== undefined) query.append('overdue', String(params.overdue));
    if (params?.search) query.append('search', params.search);
    const queryString = query.toString();
    return apiClient.get<TaskResponse[]>(`/api/projects/${projectId}/tasks${queryString ? `?${queryString}` : ''}`);
  },

  getAllTasks: (params?: { projectId?: string; status?: string; priority?: string; assigneeId?: string; overdue?: boolean; search?: string }) => {
    const query = new URLSearchParams();
    if (params?.projectId) query.append('projectId', params.projectId);
    if (params?.status) query.append('status', params.status);
    if (params?.priority) query.append('priority', params.priority);
    if (params?.assigneeId) query.append('assigneeId', params.assigneeId);
    if (params?.overdue !== undefined) query.append('overdue', String(params.overdue));
    if (params?.search) query.append('search', params.search);
    const queryString = query.toString();
    return apiClient.get<TaskResponse[]>(`/api/tasks${queryString ? `?${queryString}` : ''}`);
  },

  getTask: (taskId: string) =>
    apiClient.get<TaskResponse>(`/api/tasks/${taskId}`),

  createTask: (projectId: string, data: CreateTaskData) =>
    apiClient.post<TaskResponse>(`/api/projects/${projectId}/tasks`, data),

  updateTask: (taskId: string, data: UpdateTaskData) =>
    apiClient.patch<TaskResponse>(`/api/tasks/${taskId}`, data),

  deleteTask: (taskId: string) =>
    apiClient.delete<void>(`/api/tasks/${taskId}`),
};
