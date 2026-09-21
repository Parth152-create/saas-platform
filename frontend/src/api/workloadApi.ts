import { apiClient } from './apiClient';
import type {
  DepartmentWorkload,
  EmployeeWorkload,
  ProjectWorkload,
  WorkforceWorkloadSummary,
} from './types';

export const workloadApi = {
  getSummary: (): Promise<WorkforceWorkloadSummary> =>
    apiClient.get<WorkforceWorkloadSummary>('/api/workload/summary'),

  getWorkforceSummary: (): Promise<WorkforceWorkloadSummary> =>
    apiClient.get<WorkforceWorkloadSummary>('/api/workload/summary'),

  getEmployeeWorkloads: (): Promise<EmployeeWorkload[]> =>
    apiClient.get<EmployeeWorkload[]>('/api/workload/employees'),

  getDepartmentWorkloads: (): Promise<DepartmentWorkload[]> =>
    apiClient.get<DepartmentWorkload[]>('/api/workload/departments'),

  getProjectWorkloads: (): Promise<ProjectWorkload[]> =>
    apiClient.get<ProjectWorkload[]>('/api/workload/projects'),

  getMyWorkload: (): Promise<EmployeeWorkload> =>
    apiClient.get<EmployeeWorkload>('/api/workload/my'),
};
