import { apiClient } from './apiClient';

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
};
