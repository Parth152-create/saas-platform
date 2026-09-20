import { apiClient } from './apiClient';
import type { DepartmentSummary, Employee, EmployeeStatus, HrmStats } from './types';

export type EmployeeDto = Employee;
export type { DepartmentSummary, Employee, EmployeeStatus, HrmStats };

export interface GetEmployeesParams {
  department?: string;
  status?: EmployeeStatus;
  search?: string;
}

export interface CreateEmployeePayload {
  employeeId: string;
  name: string;
  email: string;
  department: string;
  position: string;
  status?: EmployeeStatus;
  hireDate?: string;
  phone?: string;
  workModel?: string;
  location?: string;
  manager?: string;
  avatarColor?: string;
  attendanceRate?: number;
  billableHours?: number;
}

export type UpdateEmployeePayload = Partial<CreateEmployeePayload>;

export interface CreateDepartmentPayload {
  name: string;
  lead?: string;
  budgetUtilization?: number;
}

export const hrmApi = {
  getEmployees: (params?: GetEmployeesParams): Promise<Employee[]> => {
    const query = new URLSearchParams();
    if (params?.department && params.department !== 'ALL') {
      query.set('department', params.department);
    }
    if (params?.status && params.status !== ('ALL' as unknown)) {
      query.set('status', params.status);
    }
    if (params?.search && params.search.trim()) {
      query.set('search', params.search.trim());
    }
    const qs = query.toString();
    return apiClient.get<Employee[]>(`/api/hrm/employees${qs ? `?${qs}` : ''}`);
  },

  getEmployee: (id: string): Promise<Employee> =>
    apiClient.get<Employee>(`/api/hrm/employees/${encodeURIComponent(id)}`),

  createEmployee: (payload: CreateEmployeePayload): Promise<Employee> =>
    apiClient.post<Employee>('/api/hrm/employees', payload),

  updateEmployee: (id: string, payload: UpdateEmployeePayload): Promise<Employee> =>
    apiClient.put<Employee>(`/api/hrm/employees/${encodeURIComponent(id)}`, payload),

  deleteEmployee: (id: string): Promise<void> =>
    apiClient.delete<void>(`/api/hrm/employees/${encodeURIComponent(id)}`),

  getDepartments: (): Promise<DepartmentSummary[]> =>
    apiClient.get<DepartmentSummary[]>('/api/hrm/departments'),

  createDepartment: (payload: CreateDepartmentPayload): Promise<DepartmentSummary> =>
    apiClient.post<DepartmentSummary>('/api/hrm/departments', payload),

  getStats: (): Promise<HrmStats> =>
    apiClient.get<HrmStats>('/api/hrm/stats'),
};
