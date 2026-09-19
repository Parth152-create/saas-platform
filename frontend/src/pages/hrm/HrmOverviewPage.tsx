import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Building2,
  Calendar,
  Clock,
  FileText,
  UserCheck,
  Users,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Badge } from '../../components/common/Badge';
import { MOCK_DEPARTMENTS, INITIAL_EMPLOYEES } from '../../mocks/mockHrmData';
import { hrmApi } from '../../api/hrmApi';
import type { DepartmentSummary, HrmStats } from '../../api/types';

export const HrmOverviewPage: React.FC = () => {
  const [departments, setDepartments] = useState<DepartmentSummary[]>(MOCK_DEPARTMENTS);
  const [stats, setStats] = useState<HrmStats | null>(null);

  useEffect(() => {
    hrmApi.getDepartments().then((data) => {
      if (Array.isArray(data)) setDepartments(data);
    }).catch(() => {});

    hrmApi.getStats().then((data) => {
      if (data) setStats(data);
    }).catch(() => {});
  }, []);

  const totalEmp = stats ? stats.totalEmployees : INITIAL_EMPLOYEES.length;
  const totalDepts = stats ? stats.totalDepartments : departments.length;
  const avgAttn = stats ? `${stats.averageAttendance}%` : '96.8%';
  const billable = stats ? `${stats.totalBillableHours}h Total Billable` : '164h Avg Monthly Billable';

  const modules = [
    {
      title: 'Employee Directory',
      path: '/app/hrm/employees',
      icon: Users,
      description: 'Manage staff profiles, organizational structure, positions, and active statuses.',
      stat: `${totalEmp} Profiles`,
    },
    {
      title: 'Teams & Departments',
      path: '/app/hrm/teams',
      icon: Building2,
      description: 'Department hierarchies, team leads, headcounts, and operational budgets.',
      stat: `${totalDepts} Departments`,
    },
    {
      title: 'Attendance & Leave',
      path: '/app/hrm/attendance',
      icon: UserCheck,
      description: 'Absence request approvals, PTO allowances, sick leaves, and punch clock audit.',
      stat: `${avgAttn} Avg Attendance`,
    },
    {
      title: 'Work Schedule Models',
      path: '/app/hrm/work-schedules',
      icon: Calendar,
      description: 'Fixed 40h models, flexible hours, shift rotations, and holiday calendars.',
      stat: '3 Models Active',
    },
    {
      title: 'Time & Workforce Tracking',
      path: '/app/hrm/time-tracking',
      icon: Clock,
      description: 'Daily work logs, overtime calculation, task time tracking, and productivity rates.',
      stat: billable,
    },
    {
      title: 'Employee Documents',
      path: '/app/hrm/documents',
      icon: FileText,
      description: 'Employment agreements, certifications, NDAs, and policy acknowledgment files.',
      stat: 'Verified & Encrypted',
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Human Resource Management"
        description="Comprehensive workforce operations, organizational structure, schedule management, and employee records."
      />

      {/* Modules Architecture Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {modules.map((mod) => {
          const Icon = mod.icon;
          return (
            <Link key={mod.path} to={mod.path} className="group block">
              <Card
                hoverEffect
                className="h-full flex flex-col justify-between p-6 transition-all"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="p-3 rounded-xl bg-neutral-100 text-neutral-900 dark:bg-[#1f1f1f] dark:text-neutral-100">
                      <Icon className="w-5 h-5" />
                    </div>
                    <Badge variant="default" size="sm">
                      {mod.stat}
                    </Badge>
                  </div>
                  <h3 className="text-base font-semibold text-neutral-900 dark:text-neutral-100 group-hover:text-neutral-950 dark:group-hover:text-white transition-colors">
                    {mod.title}
                  </h3>
                  <p className="mt-2 text-xs text-neutral-500 dark:text-neutral-400 leading-relaxed">
                    {mod.description}
                  </p>
                </div>

                <div className="mt-5 flex items-center gap-1 text-xs font-semibold text-neutral-900 dark:text-neutral-100 group-hover:gap-2 transition-all">
                  <span>Enter Module</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </div>
              </Card>
            </Link>
          );
        })}
      </div>

      {/* Organization Structure Snapshot */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Department Breakdown & Leads</CardTitle>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
              Active teams across your organization
            </p>
          </div>
          <Link
            to="/app/hrm/teams"
            className="text-xs font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
          >
            View all teams →
          </Link>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Department</th>
                  <th className="px-5 py-3">Department Lead</th>
                  <th className="px-5 py-3">Headcount</th>
                  <th className="px-5 py-3">Budget Utilization</th>
                  <th className="px-5 py-3">Access Level</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {departments.map((dept) => (
                  <tr
                    key={dept.name}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                  >
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                      {dept.name}
                    </td>
                    <td className="px-5 py-3.5">{dept.lead || 'Unassigned'}</td>
                    <td className="px-5 py-3.5 font-medium">{dept.headCount} members</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="w-24 bg-neutral-100 dark:bg-[#1f1f1f] h-2 rounded-full overflow-hidden">
                          <div
                            className="bg-neutral-950 dark:bg-white h-full rounded-full"
                            style={{ width: `${Math.min(dept.budgetUtilization, 100)}%` }}
                          />
                        </div>
                        <span className="font-semibold text-[11px] text-neutral-800 dark:text-neutral-200">{dept.budgetUtilization}%</span>
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant="primary" size="sm">
                        Standard
                      </Badge>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
