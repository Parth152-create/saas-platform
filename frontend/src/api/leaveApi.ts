import { apiClient } from './apiClient';
import type {
  CreateLeaveRequest,
  LeaveBalance,
  LeaveRequest,
  LeaveReviewRequest,
  LeaveStatus,
  LeaveType,
} from './types';

export interface LeaveQueryParams {
  status?: LeaveStatus;
  type?: LeaveType;
  fromDate?: string;
  toDate?: string;
  query?: string;
}

export const leaveApi = {
  getMyLeaveRequests: (): Promise<LeaveRequest[]> =>
    apiClient.get<LeaveRequest[]>('/api/leave/my'),

  getMyLeaveBalances: (): Promise<LeaveBalance[]> =>
    apiClient.get<LeaveBalance[]>('/api/leave/balances'),

  getPendingLeaveRequests: (): Promise<LeaveRequest[]> =>
    apiClient.get<LeaveRequest[]>('/api/leave/pending'),

  getAllLeaveRequests: (params?: LeaveQueryParams): Promise<LeaveRequest[]> => {
    const qs = new URLSearchParams();
    if (params?.status) qs.set('status', params.status);
    if (params?.type) qs.set('type', params.type);
    if (params?.fromDate) qs.set('fromDate', params.fromDate);
    if (params?.toDate) qs.set('toDate', params.toDate);
    if (params?.query) qs.set('query', params.query);
    const query = qs.toString();
    return apiClient.get<LeaveRequest[]>(`/api/leave${query ? `?${query}` : ''}`);
  },

  getLeaveRequest: (id: string): Promise<LeaveRequest> =>
    apiClient.get<LeaveRequest>(`/api/leave/${encodeURIComponent(id)}`),

  createLeaveRequest: (payload: CreateLeaveRequest): Promise<LeaveRequest> =>
    apiClient.post<LeaveRequest>('/api/leave', payload),

  cancelLeaveRequest: (id: string): Promise<LeaveRequest> =>
    apiClient.post<LeaveRequest>(`/api/leave/${encodeURIComponent(id)}/cancel`),

  approveLeaveRequest: (id: string, payload?: LeaveReviewRequest): Promise<LeaveRequest> =>
    apiClient.post<LeaveRequest>(`/api/leave/${encodeURIComponent(id)}/approve`, payload || {}),

  rejectLeaveRequest: (id: string, payload?: LeaveReviewRequest): Promise<LeaveRequest> =>
    apiClient.post<LeaveRequest>(`/api/leave/${encodeURIComponent(id)}/reject`, payload || {}),
};
