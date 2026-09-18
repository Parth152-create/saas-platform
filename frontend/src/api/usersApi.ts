import { apiClient } from './apiClient';
import type { InviteUserRequest, InviteUserResponse, WorkspaceUser } from './types';

export const usersApi = {
  getUsers: (): Promise<WorkspaceUser[]> =>
    apiClient.get<WorkspaceUser[]>('/api/users'),

  inviteUser: (req: InviteUserRequest): Promise<InviteUserResponse> =>
    apiClient.post<InviteUserResponse>('/api/users', req),
};

