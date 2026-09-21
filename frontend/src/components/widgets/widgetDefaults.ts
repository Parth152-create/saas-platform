import React from 'react';
import {
  BarChart3,
  CheckSquare,
  Clock,
  FileStack,
  PieChart,
  Users,
} from 'lucide-react';

export interface WidgetConfig {
  id: string;
  name: string;
  category: 'Metrics' | 'Charts' | 'Lists';
  description: string;
  icon: React.ElementType;
  visible: boolean;
}

export const DEFAULT_DASHBOARD_WIDGETS: WidgetConfig[] = [
  {
    id: 'kpis',
    name: 'Key Performance Indicators',
    category: 'Metrics',
    description: 'Active workforce count, hours logged, and claims/tasks cards.',
    icon: Users,
    visible: true,
  },
  {
    id: 'hoursChart',
    name: 'Workforce Hours Logged',
    category: 'Charts',
    description: 'Monthly distribution of logged work and billing hours.',
    icon: BarChart3,
    visible: true,
  },
  {
    id: 'attendanceChart',
    name: 'Attendance & Absence Model',
    category: 'Charts',
    description: 'Donut breakdown of present, on leave, and probation staff.',
    icon: PieChart,
    visible: true,
  },
  {
    id: 'tasksList',
    name: 'Pending Tasks Matrix',
    category: 'Lists',
    description: 'High-priority organizational and compliance tasks.',
    icon: CheckSquare,
    visible: true,
  },
  {
    id: 'recentActivity',
    name: 'Recent Activity & Audit Feed',
    category: 'Lists',
    description: 'Real-time actions, leave requests, and status changes.',
    icon: Clock,
    visible: true,
  },
  {
    id: 'claimsSummary',
    name: 'Employee Claims Overview',
    category: 'Lists',
    description: 'Recent expense, equipment, and travel reimbursement requests.',
    icon: FileStack,
    visible: true,
  },
  {
    id: 'leaveWidget',
    name: 'Leave & Approvals Tracker',
    category: 'Lists',
    description: 'Pending leave requests and personal leave balance status.',
    icon: Users,
    visible: true,
  },
  {
    id: 'workloadWidget',
    name: 'Workload & Capacity Overview',
    category: 'Metrics',
    description: 'Workforce capacity utilization, active tasks, and team load.',
    icon: BarChart3,
    visible: true,
  },
];
