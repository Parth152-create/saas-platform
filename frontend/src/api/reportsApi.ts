import { apiClient, getStoredAccessToken } from './apiClient';
import type {
  LeaveReport,
  ProjectsReport,
  TasksReport,
  WorkforceReport,
} from './types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8090';

export interface BasicReportsResponse {
  reportType: string;
  status: string;
  data: string[];
}

export interface AdvancedReportsResponse {
  reportType: string;
  status: string;
  data: string[];
}

export interface AnalyticsResponse {
  reportType: string;
  status: string;
  metrics: {
    headcountGrowth: number;
    runRate: number;
  };
}

export interface CustomWorkflowsResponse {
  reportType: string;
  status: string;
  workflows: string[];
}

export interface AdvancedExportResponse {
  exportStatus: string;
  authorizedRole: string;
  feature: string;
}

export const reportsApi = {
  getBasicReports: (): Promise<BasicReportsResponse> =>
    apiClient.get<BasicReportsResponse>('/api/reports/basic'),

  getAdvancedReports: (): Promise<AdvancedReportsResponse> =>
    apiClient.get<AdvancedReportsResponse>('/api/reports/advanced'),

  getAnalytics: (): Promise<AnalyticsResponse> =>
    apiClient.get<AnalyticsResponse>('/api/reports/analytics'),

  getCustomWorkflows: (): Promise<CustomWorkflowsResponse> =>
    apiClient.get<CustomWorkflowsResponse>('/api/reports/custom-workflows'),

  exportAdvancedReportAsAdmin: (): Promise<AdvancedExportResponse> =>
    apiClient.post<AdvancedExportResponse>('/api/reports/admin-advanced-export', {}),

  // v1.1 Real Persisted Data Reports
  getWorkforceReport: (department?: string, status?: string): Promise<WorkforceReport> => {
    const qs = new URLSearchParams();
    if (department && department !== 'ALL') qs.set('department', department);
    if (status && status !== 'ALL') qs.set('status', status);
    const query = qs.toString();
    return apiClient.get<WorkforceReport>(`/api/reports/workforce${query ? `?${query}` : ''}`);
  },

  getProjectsReport: (status?: string, priority?: string): Promise<ProjectsReport> => {
    const qs = new URLSearchParams();
    if (status && status !== 'ALL') qs.set('status', status);
    if (priority && priority !== 'ALL') qs.set('priority', priority);
    const query = qs.toString();
    return apiClient.get<ProjectsReport>(`/api/reports/projects${query ? `?${query}` : ''}`);
  },

  getTasksReport: (projectId?: string, status?: string, priority?: string): Promise<TasksReport> => {
    const qs = new URLSearchParams();
    if (projectId && projectId !== 'ALL') qs.set('projectId', projectId);
    if (status && status !== 'ALL') qs.set('status', status);
    if (priority && priority !== 'ALL') qs.set('priority', priority);
    const query = qs.toString();
    return apiClient.get<TasksReport>(`/api/reports/tasks${query ? `?${query}` : ''}`);
  },

  getLeaveReport: (status?: string, leaveType?: string, fromDate?: string, toDate?: string): Promise<LeaveReport> => {
    const qs = new URLSearchParams();
    if (status && status !== 'ALL') qs.set('status', status);
    if (leaveType && leaveType !== 'ALL') qs.set('leaveType', leaveType);
    if (fromDate) qs.set('fromDate', fromDate);
    if (toDate) qs.set('toDate', toDate);
    const query = qs.toString();
    return apiClient.get<LeaveReport>(`/api/reports/leave${query ? `?${query}` : ''}`);
  },

  downloadCsvExport: async (type: 'workforce' | 'projects' | 'tasks' | 'leave'): Promise<void> => {
    const token = getStoredAccessToken();
    const res = await fetch(`${API_BASE_URL}/api/reports/export/${type}`, {
      method: 'GET',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });

    if (!res.ok) {
      throw new Error(`Failed to export ${type} report: ${res.statusText}`);
    }

    const blob = await res.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `report-${type}-${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  },
};
