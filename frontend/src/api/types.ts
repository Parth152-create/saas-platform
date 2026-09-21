// Type definitions exactly matching Spring Boot DTOs and entities

export type Role = 'SUPER_ADMIN' | 'ADMIN' | 'MANAGER' | 'USER';

export interface UserProfile {
  id: string;
  email: string;
  role: Role;
  tenantId: string;
}

export interface SignupRequest {
  tenantId: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  tenantId: string;
  email: string;
  password: string;
}

export interface GoogleLoginRequest {
  idToken: string;
  tenantId: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AcceptInviteRequest {
  tenantId: string;
  token: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface InviteUserRequest {
  email: string;
  role: Role;
}

export interface InviteUserResponse {
  email: string;
  role: Role;
  inviteToken: string;
  expiresAt: string;
}

export type PlanTier = 'FREE' | 'STARTER' | 'PRO' | 'ENTERPRISE';

export type Feature =
  | 'EMPLOYEE_MANAGEMENT'
  | 'TEAM_MANAGEMENT'
  | 'PROJECT_MANAGEMENT'
  | 'TASK_MANAGEMENT'
  | 'CLAIMS'
  | 'TIME_TRACKING'
  | 'ATTENDANCE'
  | 'LEAVE_MANAGEMENT'
  | 'WORK_SCHEDULES'
  | 'DOCUMENTS'
  | 'BASIC_REPORTS'
  | 'ADVANCED_REPORTS'
  | 'ADVANCED_ANALYTICS'
  | 'ADVANCED_HRM'
  | 'CUSTOM_WORKFLOWS'
  | 'ADVANCED_INTEGRATIONS'
  | 'TEAM_CHAT'
  | 'NOTIFICATIONS'
  | 'CALENDAR';

export interface FeatureEntitlementsResponse {
  plan: string;
  status: string;
  features: Feature[];
}

export type SubscriptionStatus =
  | 'ACTIVE'
  | 'TRIALING'
  | 'CANCELED'
  | 'INCOMPLETE'
  | 'PAST_DUE'
  | 'UNPAID';

export type InvoiceStatus = 'DRAFT' | 'OPEN' | 'PAID' | 'UNCOLLECTIBLE' | 'VOID';

export interface SubscriptionResponse {
  status: SubscriptionStatus;
  stripeSubscriptionId: string;
  stripePriceId: string;
  currentPeriodStart: string;
  currentPeriodEnd: string;
  cancelAtPeriodEnd: boolean;
}

export interface InvoiceResponse {
  stripeInvoiceId: string;
  status: InvoiceStatus;
  amountDueCents: number;
  amountPaidCents: number;
  currency: string;
  hostedInvoiceUrl?: string;
  invoicePdfUrl?: string;
  paidAt?: string;
}

export interface BillingSummaryResponse {
  plan: string;
  subscription: SubscriptionResponse | null;
  invoices: InvoiceResponse[];
}

export interface CreateCheckoutSessionRequest {
  planTier: PlanTier;
}

export interface CreateCheckoutSessionResponse {
  checkoutUrl: string;
}

export interface CreatePortalSessionResponse {
  url: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
  requestId?: string;
}

export type EmployeeStatus = 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE' | 'PROBATION' | 'TERMINATED';

export interface Employee {
  id: string;
  employeeId: string;
  name: string;
  email: string;
  department: string;
  position: string;
  status: EmployeeStatus;
  hireDate: string;
  phone?: string;
  workModel: string;
  location?: string;
  manager?: string;
  avatarColor?: string;
  attendanceRate: number;
  billableHours: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface DepartmentSummary {
  id?: string;
  name: string;
  lead?: string;
  budgetUtilization: number;
  headCount: number;
}

export interface HrmStats {
  totalEmployees: number;
  activeEmployees: number;
  onLeaveEmployees: number;
  probationEmployees: number;
  inactiveEmployees: number;
  totalDepartments: number;
  averageAttendance: number;
  totalBillableHours: number;
}

export interface ChangeRoleRequest {
  role: Role;
}

export interface WorkspaceUser {
  id: string;
  email: string;
  role: Role;
  status: 'ACTIVE' | 'INVITED' | 'DISABLED' | 'SUSPENDED';
  inviteToken?: string;
  inviteTokenExpiresAt?: string;
  createdAt: string;
}

// ==========================================
// Nexa v1.1 Product Additions
// ==========================================

export type LeaveType = 'ANNUAL' | 'SICK' | 'CASUAL' | 'UNPAID' | 'PARENTAL' | 'BEREAVEMENT' | 'OTHER';
export type LeaveStatus = 'DRAFT' | 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface LeaveRequest {
  id: string;
  userId: string;
  employeeId?: string | null;
  employeeName: string;
  employeeEmail: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  daysCount: number;
  reason: string;
  status: LeaveStatus;
  reviewerId?: string | null;
  reviewerName?: string | null;
  reviewNote?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LeaveBalance {
  id: string;
  userId: string;
  year: number;
  leaveType: LeaveType;
  totalDays: number;
  usedDays: number;
  pendingDays: number;
  remainingDays: number;
}

export interface CreateLeaveRequest {
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  reason: string;
}

export interface LeaveReviewRequest {
  reviewNote?: string;
}

export interface SelfServiceProfile {
  userId: string;
  email: string;
  role: Role;
  tenantId: string;
  employeeId?: string | null;
  employeeCode?: string | null;
  name: string;
  department: string;
  position: string;
  status: EmployeeStatus;
  hireDate: string;
  phone?: string | null;
  workModel: string;
  location?: string | null;
  manager?: string | null;
  avatarColor?: string | null;
  attendanceRate: number;
  billableHours: number;
}

export interface UpdateSelfServiceProfileRequest {
  name?: string;
  phone?: string;
  location?: string;
  workModel?: string;
}

export interface SelfServiceTask {
  id: string;
  title: string;
  projectId?: string;
  projectName?: string;
  status: string;
  priority: string;
  dueDate?: string | null;
}

export interface SelfServiceProject {
  id: string;
  name: string;
  status: string;
  client?: string | null;
  role?: string | null;
}

export interface SelfServiceNotification {
  id: string;
  title: string;
  message?: string;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface SelfServiceCalendarEvent {
  id: string;
  title: string;
  description?: string;
  startTime?: string;
  endTime?: string;
}

export interface SelfServiceOverview {
  profile: SelfServiceProfile;
  assignedTasks: SelfServiceTask[];
  assignedProjects: SelfServiceProject[];
  leaveBalances: LeaveBalance[];
  recentLeaveRequests: LeaveRequest[];
  recentNotifications: SelfServiceNotification[];
  upcomingEvents: SelfServiceCalendarEvent[];
}

export interface EmployeeWorkload {
  employeeOrUserId: string;
  name: string;
  email: string;
  department: string;
  totalTasks: number;
  openTasks: number;
  completedTasks: number;
  overdueTasks: number;
  dueSoonTasks: number;
  estimatedHoursTotal: number;
  estimatedHoursRemaining: number;
  actualHours: number;
  weeklyCapacityHours: number;
  capacityUtilization: number;
  workloadStatus: 'NORMAL' | 'OPTIMAL' | 'HIGH' | 'OVERLOADED';
}

export interface DepartmentWorkload {
  department: string;
  employeeCount: number;
  totalTasks: number;
  openTasks: number;
  overdueTasks: number;
  totalEstimatedHours: number;
  totalActualHours: number;
  averageUtilization: number;
}

export interface ProjectWorkload {
  projectId: string;
  projectName: string;
  status: string;
  totalTasks: number;
  openTasks: number;
  completedTasks: number;
  overdueTasks: number;
  estimatedHours: number;
  actualHours: number;
  progressPercentage: number;
}

export interface WorkforceWorkloadSummary {
  totalEmployees: number;
  totalTasks: number;
  openTasks: number;
  completedTasks: number;
  overdueTasks: number;
  dueSoonTasks: number;
  totalEstimatedHours: number;
  totalActualHours: number;
  standardWeeklyCapacityHours: number;
  averageUtilizationPercentage: number;
  employeeWorkloads: EmployeeWorkload[];
  departmentWorkloads: DepartmentWorkload[];
  projectWorkloads: ProjectWorkload[];
}

export interface WorkforceReport {
  totalEmployees: number;
  activeEmployees: number;
  inactiveEmployees: number;
  onLeaveEmployees: number;
  probationEmployees: number;
  roleDistribution: Record<string, number>;
  departmentDistribution: Record<string, number>;
  employees: {
    id: string;
    employeeId: string;
    name: string;
    email: string;
    department: string;
    position: string;
    status: string;
    hireDate: string;
    attendanceRate: number;
    billableHours: number;
  }[];
}

export interface ProjectsReport {
  totalProjects: number;
  activeProjects: number;
  completedProjects: number;
  planningProjects: number;
  onHoldProjects: number;
  archivedProjects: number;
  projectsByStatus: Record<string, number>;
  projectsByPriority: Record<string, number>;
  projects: {
    id: string;
    name: string;
    client?: string | null;
    status: string;
    priority: string;
    startDate?: string | null;
    dueDate?: string | null;
    budget?: number | null;
    totalTasks: number;
    doneTasks: number;
    progressPercentage: number;
  }[];
}

export interface TasksReport {
  totalTasks: number;
  openTasks: number;
  completedTasks: number;
  overdueTasks: number;
  completionRate: number;
  tasksByStatus: Record<string, number>;
  tasksByPriority: Record<string, number>;
  tasks: {
    id: string;
    title: string;
    projectName: string;
    status: string;
    priority: string;
    assigneeName?: string | null;
    dueDate?: string | null;
    estimatedHours?: number | null;
    actualHours?: number | null;
  }[];
}

export interface LeaveReport {
  totalRequests: number;
  pendingRequests: number;
  approvedRequests: number;
  rejectedRequests: number;
  cancelledRequests: number;
  totalDaysApproved: number;
  requestsByStatus: Record<string, number>;
  requestsByType: Record<string, number>;
  requests: {
    id: string;
    employeeName: string;
    employeeEmail: string;
    leaveType: string;
    startDate: string;
    endDate: string;
    daysCount: number;
    status: string;
    reviewerName?: string | null;
    reviewedAt?: string | null;
  }[];
}

export interface SearchResult {
  id: string;
  type: 'EMPLOYEE' | 'PROJECT' | 'TASK' | 'TEAM' | 'LEAVE';
  title: string;
  subtitle: string;
  description: string;
  targetUrl: string;
  status: string;
}

export interface BulkOperationResult {
  totalRequested: number;
  successCount: number;
  failureCount: number;
  successIds: string[];
  errors: { id: string; error: string }[];
}
