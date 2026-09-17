import React from 'react';
import {
  Users,
  CheckSquare,
  Clock,
  BarChart3,
  Receipt,
  FileText,
  ArrowRight,
} from 'lucide-react';

interface PlatformModule {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  badge: string;
  description: string;
  tags: string[];
}

const MODULES: PlatformModule[] = [
  {
    icon: Users,
    title: 'Workforce & HR',
    badge: 'Core HRM',
    description:
      'Manage complete employee profiles, department structures, leave requests, attendance logs, and work shifts in one unified directory.',
    tags: ['Employee Directory', 'Leave Balances', 'Shift Scheduling'],
  },
  {
    icon: CheckSquare,
    title: 'Projects & Tasks',
    badge: 'Operations',
    description:
      'Structure work across multi-stage lifecycles, assign tasks with priority levels, and keep cross-functional initiatives on schedule.',
    tags: ['Project Tracking', 'Task Pipelines', 'Priority Management'],
  },
  {
    icon: Clock,
    title: 'Time & Attendance',
    badge: 'Time Tracking',
    description:
      'Log billable and operational hours directly against projects, verify attendance records, and generate clean time utilization reports.',
    tags: ['Work Hours', 'Clock In / Out', 'Timesheets'],
  },
  {
    icon: BarChart3,
    title: 'Reports & Analytics',
    badge: 'Intelligence',
    description:
      'Gain immediate visibility into department headcount, attendance trends, project hour utilization, and operational performance.',
    tags: ['Operational KPI Reports', 'Exportable Data', 'Trend Analysis'],
  },
  {
    icon: Receipt,
    title: 'Claims & Expenses',
    badge: 'Finance',
    description:
      'Streamline business expense submissions with categorized claims, receipt documentation, and direct manager approval workflows.',
    tags: ['Expense Submission', 'Receipt Records', 'Approval Flow'],
  },
  {
    icon: FileText,
    title: 'Documents & Records',
    badge: 'Compliance',
    description:
      'Centralize company documentation, contracts, and workforce records within strictly isolated, tenant-partitioned storage.',
    tags: ['Secure File Records', 'Role Permissions', 'Audit Trails'],
  },
];

export const PlatformOverview: React.FC = () => {
  return (
    <section id="platform" className="py-24 bg-white dark:bg-[#0a0a0a] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="max-w-3xl mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-100 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-200 dark:border-[#262626] mb-4">
            Unified Architecture
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Everything your team needs.
            <br />
            One connected workspace.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            Eliminate fragmented tools and duplicate data entry. Every module connects to the same
            tenant schema for unified reporting and real-time operational governance.
          </p>
        </div>

        {/* Modules Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {MODULES.map((module) => {
            const Icon = module.icon;
            return (
              <div
                key={module.title}
                className="group relative flex flex-col justify-between p-6 rounded-xl bg-neutral-50 dark:bg-[#121212] border border-neutral-200 dark:border-[#262626] hover:border-neutral-400 dark:hover:border-neutral-600 transition-all duration-300"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-white dark:bg-[#1a1a1a] border border-neutral-200 dark:border-[#262626] text-neutral-900 dark:text-neutral-100 group-hover:bg-neutral-950 group-hover:text-white dark:group-hover:bg-white dark:group-hover:text-neutral-950 transition-colors">
                      <Icon className="w-5 h-5" />
                    </div>
                    <span className="text-[11px] font-semibold tracking-wider uppercase px-2.5 py-0.5 rounded-full bg-neutral-200/60 dark:bg-[#1f1f1f] text-neutral-700 dark:text-neutral-300">
                      {module.badge}
                    </span>
                  </div>

                  <h3 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100 mb-2">
                    {module.title}
                  </h3>
                  <p className="text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed mb-6">
                    {module.description}
                  </p>
                </div>

                <div className="pt-4 border-t border-neutral-200/80 dark:border-[#262626]/80 flex flex-wrap gap-1.5">
                  {module.tags.map((tag) => (
                    <span
                      key={tag}
                      className="text-[11px] font-medium text-neutral-500 dark:text-neutral-400 bg-white dark:bg-[#181818] px-2 py-0.5 rounded border border-neutral-200 dark:border-[#262626]"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              </div>
            );
          })}
        </div>

        {/* Sub-banner */}
        <div className="mt-12 p-6 rounded-xl bg-neutral-100 dark:bg-[#161616] border border-neutral-200 dark:border-[#262626] flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="text-center sm:text-left">
            <h4 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
              Need custom integrations or bespoke organizational workflows?
            </h4>
            <p className="text-xs text-neutral-600 dark:text-neutral-400 mt-0.5">
              Enterprise plans include custom workflow triggers and dedicated webhook hooks.
            </p>
          </div>
          <a
            href="#pricing"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-neutral-900 dark:text-neutral-100 hover:underline shrink-0"
          >
            Explore enterprise tiers <ArrowRight className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>
    </section>
  );
};
