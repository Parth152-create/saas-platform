import { apiClient } from './apiClient';
import type { BulkOperationResult } from './types';

export interface BulkTaskStatusPayload {
  taskIds: string[];
  status: string;
}

export interface BulkTaskAssignPayload {
  taskIds: string[];
  assigneeId: string;
}

export interface BulkUserStatusPayload {
  userIds: string[];
  status: 'ACTIVE' | 'DISABLED';
}

export const bulkApi = {
  bulkUpdateTaskStatus: (payload: BulkTaskStatusPayload): Promise<BulkOperationResult> =>
    apiClient.post<BulkOperationResult>('/api/bulk/tasks/status', payload),

  updateTaskStatus: (taskIds: string[], status: string): Promise<BulkOperationResult> =>
    apiClient.post<BulkOperationResult>('/api/bulk/tasks/status', { taskIds, status }),

  bulkAssignTasks: (payload: BulkTaskAssignPayload): Promise<BulkOperationResult> =>
    apiClient.post<BulkOperationResult>('/api/bulk/tasks/assign', payload),

  assignTasks: (taskIds: string[], assigneeId: string): Promise<BulkOperationResult> =>
    apiClient.post<BulkOperationResult>('/api/bulk/tasks/assign', { taskIds, assigneeId }),

  bulkUpdateUserStatus: (payload: BulkUserStatusPayload): Promise<BulkOperationResult> =>
    apiClient.post<BulkOperationResult>('/api/bulk/users/status', payload),

  updateUserStatus: (userIds: string[], status: 'ACTIVE' | 'DISABLED'): Promise<BulkOperationResult> =>
    apiClient.post<BulkOperationResult>('/api/bulk/users/status', { userIds, status }),
};
