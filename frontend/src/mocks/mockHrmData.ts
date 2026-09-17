// Isolated mock data for SchadenPro UI modules where backend APIs do not yet exist
// Clearly separated from real backend tenant data per FRONTEND_SPEC.md

export interface Employee {
  id: string;
  name: string;
  employeeId: string;
  email: string;
  department: string;
  position: string;
  status: 'ACTIVE' | 'ON_LEAVE' | 'INACTIVE' | 'PROBATION' | 'TERMINATED';
  hireDate: string;
  phone: string;
  workModel: 'Remote' | 'Hybrid' | 'On-Site';
  location: string;
  manager: string;
  avatarColor: string;
  attendanceRate: number;
  billableHours: number;
}

export const INITIAL_EMPLOYEES: Employee[] = [
  {
    id: 'emp-101',
    name: 'Alexander Chen',
    employeeId: 'EMP-0491',
    email: 'alexander.chen@workspace.io',
    department: 'Engineering',
    position: 'Lead Systems Architect',
    status: 'ACTIVE',
    hireDate: '2023-04-12',
    phone: '+1 (555) 234-5678',
    workModel: 'Hybrid',
    location: 'San Francisco, CA',
    manager: 'Sarah Jenkins',
    avatarColor: 'bg-zinc-800 text-zinc-100',
    attendanceRate: 98.5,
    billableHours: 164,
  },
  {
    id: 'emp-102',
    name: 'Elena Rostova',
    employeeId: 'EMP-0512',
    email: 'elena.rostova@workspace.io',
    department: 'Product & Design',
    position: 'Principal UI/UX Designer',
    status: 'ACTIVE',
    hireDate: '2023-08-15',
    phone: '+1 (555) 345-6789',
    workModel: 'Remote',
    location: 'New York, NY',
    manager: 'Michael Vance',
    avatarColor: 'bg-zinc-700 text-zinc-100',
    attendanceRate: 99.1,
    billableHours: 152,
  },
  {
    id: 'emp-103',
    name: 'Marcus Sterling',
    employeeId: 'EMP-0628',
    email: 'marcus.sterling@workspace.io',
    department: 'Operations',
    position: 'Senior Operations Lead',
    status: 'ON_LEAVE',
    hireDate: '2024-01-10',
    phone: '+1 (555) 456-7890',
    workModel: 'On-Site',
    location: 'Austin, TX',
    manager: 'Sarah Jenkins',
    avatarColor: 'bg-zinc-900 text-zinc-100',
    attendanceRate: 92.4,
    billableHours: 130,
  },
  {
    id: 'emp-104',
    name: 'Sophia Martinez',
    employeeId: 'EMP-0734',
    email: 'sophia.martinez@workspace.io',
    department: 'Engineering',
    position: 'Senior Backend Engineer',
    status: 'ACTIVE',
    hireDate: '2024-03-22',
    phone: '+1 (555) 567-8901',
    workModel: 'Remote',
    location: 'Chicago, IL',
    manager: 'Alexander Chen',
    avatarColor: 'bg-zinc-800 text-zinc-100',
    attendanceRate: 97.8,
    billableHours: 168,
  },
  {
    id: 'emp-105',
    name: 'David Okafor',
    employeeId: 'EMP-0845',
    email: 'david.okafor@workspace.io',
    department: 'Sales & Accounts',
    position: 'Enterprise Account Executive',
    status: 'ACTIVE',
    hireDate: '2024-06-01',
    phone: '+1 (555) 678-9012',
    workModel: 'Hybrid',
    location: 'Seattle, WA',
    manager: 'Rachel Adams',
    avatarColor: 'bg-zinc-700 text-zinc-100',
    attendanceRate: 96.2,
    billableHours: 145,
  },
  {
    id: 'emp-106',
    name: 'Emily Thornton',
    employeeId: 'EMP-0919',
    email: 'emily.thornton@workspace.io',
    department: 'Human Resources',
    position: 'People Operations Manager',
    status: 'PROBATION',
    hireDate: '2026-08-01',
    phone: '+1 (555) 789-0123',
    workModel: 'Hybrid',
    location: 'Denver, CO',
    manager: 'Sarah Jenkins',
    avatarColor: 'bg-zinc-900 text-zinc-100',
    attendanceRate: 100,
    billableHours: 120,
  },
];

export interface DashboardKpis {
  totalEmployees: number;
  activeEmployees: number;
  onLeave: number;
  openClaims: number;
  pendingTasks: number;
  hoursLoggedThisMonth: number;
}

export const MOCK_DASHBOARD_KPIS: DashboardKpis = {
  totalEmployees: 48,
  activeEmployees: 44,
  onLeave: 3,
  openClaims: 12,
  pendingTasks: 27,
  hoursLoggedThisMonth: 6420,
};

export interface ActivityItem {
  id: string;
  user: string;
  action: string;
  target: string;
  timestamp: string;
  type: 'info' | 'success' | 'warning';
}

export const MOCK_ACTIVITIES: ActivityItem[] = [
  {
    id: 'act-1',
    user: 'Alexander Chen',
    action: 'submitted work log',
    target: 'Sprint 24 Architecture',
    timestamp: '15 minutes ago',
    type: 'info',
  },
  {
    id: 'act-2',
    user: 'Elena Rostova',
    action: 'approved design specs',
    target: 'Component Design Tokens v2',
    timestamp: '1 hour ago',
    type: 'success',
  },
  {
    id: 'act-3',
    user: 'Marcus Sterling',
    action: 'requested PTO leave',
    target: '3 days (Oct 4 - Oct 7)',
    timestamp: '3 hours ago',
    type: 'warning',
  },
  {
    id: 'act-4',
    user: 'Sophia Martinez',
    action: 'closed task ticket',
    target: 'Fix DB Connection Pool Drain',
    timestamp: 'Yesterday at 4:30 PM',
    type: 'success',
  },
];

export interface TaskItem {
  id: string;
  title: string;
  assignee: string;
  dueDate: string;
  priority: 'High' | 'Medium' | 'Low';
  status: 'In Progress' | 'Completed' | 'Pending Review';
}

export const MOCK_TASKS: TaskItem[] = [
  {
    id: 'tsk-1',
    title: 'Complete Q3 Security Compliance Audit',
    assignee: 'Alexander Chen',
    dueDate: '2026-09-30',
    priority: 'High',
    status: 'In Progress',
  },
  {
    id: 'tsk-2',
    title: 'Review New Onboarding Work Schedule Models',
    assignee: 'Emily Thornton',
    dueDate: '2026-10-05',
    priority: 'Medium',
    status: 'Pending Review',
  },
  {
    id: 'tsk-3',
    title: 'Stripe Billing Portal Customer Sync Verification',
    assignee: 'Sophia Martinez',
    dueDate: '2026-10-02',
    priority: 'High',
    status: 'In Progress',
  },
  {
    id: 'tsk-4',
    title: 'Update Employee Handbook & Policy Documents',
    assignee: 'Emily Thornton',
    dueDate: '2026-10-15',
    priority: 'Low',
    status: 'Completed',
  },
];

export interface DepartmentSummary {
  name: string;
  headCount: number;
  lead: string;
  budgetUtilization: number;
}

export const MOCK_DEPARTMENTS: DepartmentSummary[] = [
  { name: 'Engineering', headCount: 18, lead: 'Alexander Chen', budgetUtilization: 78 },
  { name: 'Product & Design', headCount: 8, lead: 'Elena Rostova', budgetUtilization: 64 },
  { name: 'Operations', headCount: 10, lead: 'Marcus Sterling', budgetUtilization: 82 },
  { name: 'Sales & Accounts', headCount: 7, lead: 'David Okafor', budgetUtilization: 91 },
  { name: 'Human Resources', headCount: 5, lead: 'Emily Thornton', budgetUtilization: 45 },
];

export interface ScheduleModel {
  id: string;
  name: string;
  weeklyHours: number;
  daysPerWeek: number;
  type: 'Fixed' | 'Flexible' | 'Shift';
  assignedEmployees: number;
}

export const MOCK_SCHEDULE_MODELS: ScheduleModel[] = [
  { id: 'sm-1', name: 'Standard Full-Time (40h Mon-Fri)', weeklyHours: 40, daysPerWeek: 5, type: 'Fixed', assignedEmployees: 32 },
  { id: 'sm-2', name: 'Flexible Core Hours (37.5h)', weeklyHours: 37.5, daysPerWeek: 5, type: 'Flexible', assignedEmployees: 11 },
  { id: 'sm-3', name: '4-Day Compressed Workweek (36h)', weeklyHours: 36, daysPerWeek: 4, type: 'Flexible', assignedEmployees: 5 },
];

export interface AbsenceType {
  id: string;
  name: string;
  allowanceDays: number;
  paid: boolean;
  requiresApproval: boolean;
}

export const MOCK_ABSENCE_TYPES: AbsenceType[] = [
  { id: 'abs-1', name: 'Paid Time Off (Annual Vacation)', allowanceDays: 20, paid: true, requiresApproval: true },
  { id: 'abs-2', name: 'Sick & Medical Leave', allowanceDays: 10, paid: true, requiresApproval: false },
  { id: 'abs-3', name: 'Parental Leave', allowanceDays: 60, paid: true, requiresApproval: true },
  { id: 'abs-4', name: 'Bereavement Leave', allowanceDays: 5, paid: true, requiresApproval: false },
  { id: 'abs-5', name: 'Unpaid Leave of Absence', allowanceDays: 30, paid: false, requiresApproval: true },
];

export interface ProjectItem {
  id: string;
  name: string;
  client: string;
  status: 'In Progress' | 'On Track' | 'At Risk' | 'Delivered';
  progress: number;
  dueDate: string;
  budget: string;
}

export const MOCK_PROJECTS: ProjectItem[] = [
  { id: 'prj-1', name: 'Multi-Tenant Schema Migration Pipeline', client: 'Internal Platform', status: 'In Progress', progress: 85, dueDate: '2026-10-15', budget: '$45,000' },
  { id: 'prj-2', name: 'Enterprise Billing & Stripe Webhook Engine', client: 'Acme Corp', status: 'On Track', progress: 100, dueDate: '2026-09-20', budget: '$62,000' },
  { id: 'prj-3', name: 'Workforce Attendance & Absence Tracker', client: 'SchadenPro Partner', status: 'In Progress', progress: 65, dueDate: '2026-11-01', budget: '$38,000' },
  { id: 'prj-4', name: 'Single Sign-On & Google OIDC Verification', client: 'Security Taskforce', status: 'Delivered', progress: 100, dueDate: '2026-09-01', budget: '$20,000' },
];

export interface ClaimItem {
  id: string;
  claimNumber: string;
  claimant: string;
  category: string;
  amount: number;
  dateFiled: string;
  status: 'Approved' | 'Under Review' | 'Pending Approval' | 'Rejected';
}

export const MOCK_CLAIMS: ClaimItem[] = [
  { id: 'clm-1', claimNumber: 'CLM-2026-891', claimant: 'Alexander Chen', category: 'Hardware & Ergonomics', amount: 480.00, dateFiled: '2026-09-12', status: 'Approved' },
  { id: 'clm-2', claimNumber: 'CLM-2026-892', claimant: 'David Okafor', category: 'Client Travel & Accommodation', amount: 1250.50, dateFiled: '2026-09-14', status: 'Under Review' },
  { id: 'clm-3', claimNumber: 'CLM-2026-893', claimant: 'Elena Rostova', category: 'Design Software Subscriptions', amount: 180.00, dateFiled: '2026-09-16', status: 'Approved' },
  { id: 'clm-4', claimNumber: 'CLM-2026-894', claimant: 'Sophia Martinez', category: 'Conference Registration', amount: 650.00, dateFiled: '2026-09-17', status: 'Pending Approval' },
];
