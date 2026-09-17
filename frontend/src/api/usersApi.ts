import { apiClient } from './apiClient';
import type { InviteUserRequest, InviteUserResponse } from './types';

export const usersApi = {
  inviteUser: (req: InviteUserRequest): Promise<InviteUserResponse> =>
    apiClient.post<InviteUserResponse>('/api/users', req),
};
