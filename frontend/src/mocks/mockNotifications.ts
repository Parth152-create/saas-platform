// Isolated mock notifications for UI modules where backend notification APIs do not yet exist
// Follows FRONTEND_SPEC.md Section 22 and PROJECT_SPEC.md Section 41

import type { AppNotification } from '../types/notification';

export const INITIAL_NOTIFICATIONS: AppNotification[] = [
  {
    id: 'notif-1',
    title: 'Employee invitation accepted',
    description: 'Elena Rostova accepted your workspace invitation and joined the Product & Design team.',
    timestamp: '5 minutes ago',
    createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
    read: false,
    category: 'employee',
    targetPath: '/app/hrm/employees',
  },
  {
    id: 'notif-2',
    title: 'Task "Security Audit" completed',
    description: 'Alexander Chen completed the Q3 Security Compliance Audit.',
    timestamp: '1 hour ago',
    createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    read: true,
    category: 'task',
    targetPath: '/app/tasks',
  },
  {
    id: 'notif-3',
    title: 'New expense claim submitted',
    description: 'Sophia Martinez submitted expense claim CLM-2026-894 ($650.00) for review.',
    timestamp: '3 hours ago',
    createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
    read: true,
    category: 'claim',
    targetPath: '/app/claims',
  },
];
