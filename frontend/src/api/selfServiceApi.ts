import { apiClient } from './apiClient';
import type {
  SelfServiceOverview,
  SelfServiceProfile,
  UpdateSelfServiceProfileRequest,
} from './types';
import type { ProjectResponse, TaskResponse } from './projectsApi';

export const selfServiceApi = {
  getProfile: (): Promise<SelfServiceProfile> =>
    apiClient.get<SelfServiceProfile>('/api/self-service/profile'),

  updateProfile: (payload: UpdateSelfServiceProfileRequest): Promise<SelfServiceProfile> =>
    apiClient.put<SelfServiceProfile>('/api/self-service/profile', payload),

  getAssignedTasks: (): Promise<TaskResponse[]> =>
    apiClient.get<TaskResponse[]>('/api/self-service/tasks'),

  getAssignedProjects: (): Promise<ProjectResponse[]> =>
    apiClient.get<ProjectResponse[]>('/api/self-service/projects'),

  getOverview: (): Promise<SelfServiceOverview> =>
    apiClient.get<SelfServiceOverview>('/api/self-service/overview'),
};
