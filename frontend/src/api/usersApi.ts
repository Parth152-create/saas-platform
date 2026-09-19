import { apiClient } from './apiClient';
import type { InviteUserRequest, InviteUserResponse, WorkspaceUser } from './types';

export const usersApi = {
  getUsers: (): Promise<WorkspaceUser[]> =>
    apiClient.get<WorkspaceUser[]>('/api/users'),

  inviteUser: (req: InviteUserRequest): Promise<InviteUserResponse> =>
    apiClient.post<InviteUserResponse>('/api/users', req),

  changeRole: (userId: string, role: import('./types').Role): Promise<WorkspaceUser> =>
    apiClient.patch<WorkspaceUser>(`/api/users/${userId}/role`, { role }),

  deactivateUser: (userId: string): Promise<WorkspaceUser> =>
    apiClient.delete<WorkspaceUser>(`/api/users/${userId}`),
};

