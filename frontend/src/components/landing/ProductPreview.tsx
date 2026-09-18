import React from 'react';
import {
  Calendar,
  CheckCircle2,
  CheckSquare,
  Clock,
  FileStack,
  FolderKanban,
  LayoutDashboard,
  Search,
  UserCheck,
  Users,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../common/Card';
import { Badge } from '../common/Badge';
import { SimpleBarChart, SimpleDonutChart } from '../widgets/SimpleChart';

export const ProductPreview: React.FC = () => {
  return (
    <section id="product" className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pb-20 sm:pb-28">
      {/* Browser Window Wrapper */}
      <div className="rounded-2xl border border-neutral-200 dark:border-[#262626] bg-neutral-100 dark:bg-[#111111] p-2 sm:p-3 shadow-2xl transition-all">
        {/* Window Topbar */}
        <div className="flex items-center justify-between px-3 py-2 border-b border-neutral-200/80 dark:border-[#222222] bg-white/70 dark:bg-[#161616]/70 rounded-t-xl backdrop-blur-xs">
          <div className="flex items-center gap-2">
            <span className="h-3 w-3 rounded-full bg-red-400/80" />
            <span className="h-3 w-3 rounded-full bg-amber-400/80" />
            <span className="h-3 w-3 rounded-full bg-emerald-400/80" />
            <span className="text-[11px] font-mono text-neutral-400 ml-2 hidden sm:inline">
              app.nexa.internal/workspace/dashboard
            </span>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-neutral-100 dark:bg-[#222222] text-[10px] font-mono text-neutral-500">
              <Search className="w-3 h-3" />
              <span>⌘K Quick search...</span>
            </div>
            <Badge variant="success" size="sm" withDot>
              LIVE DEMO
            </Badge>
          </div>
        </div>

        {/* Application Mockup Body */}
        <div className="p-4 sm:p-6 bg-white dark:bg-[#0a0a0a] rounded-b-xl space-y-6">
          {/* Header Row */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-neutral-100 dark:border-[#1f1f1f]">
            <div>
              <div className="flex items-center gap-2">
                <LayoutDashboard className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
                <h2 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                  Operations Overview
                </h2>
                <Badge variant="primary" size="sm">
                  Acme Corp (PRO)
                </Badge>
              </div>
              <p className="text-xs text-neutral-500 mt-0.5">
                Real-time operational health, headcount allocation, and attendance tracking.
              </p>
            </div>
            <div className="flex items-center gap-2 text-xs text-neutral-500">
              <Clock className="w-3.5 h-3.5" />
              <span>Updated just now</span>
            </div>
          </div>

          {/* KPI Cards Row */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
            {[
              {
                title: 'Total Workforce',
                value: '48 Staff',
                change: '+4.2% mo/mo',
                icon: Users,
                color: 'text-neutral-900 dark:text-neutral-100',
              },
              {
                title: 'Active Today',
                value: '44 Present',
                change: '91.6% capacity',
                icon: UserCheck,
                color: 'text-emerald-600 dark:text-emerald-400',
              },
              {
                title: 'Hours Logged',
                value: '6,420 hrs',
                change: 'On schedule',
                icon: Clock,
                color: 'text-neutral-900 dark:text-neutral-100',
              },
              {
                title: 'Active Projects',
                value: '12 Client',
                change: '100% SLA',
                icon: FolderKanban,
                color: 'text-purple-600 dark:text-purple-400',
              },
            ].map((kpi) => {
              const Icon = kpi.icon;
              return (
                <div
                  key={kpi.title}
                  className="p-3.5 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414]"
                >
                  <div className="flex items-center justify-between text-neutral-500 mb-1.5">
                    <span className="text-[11px] font-medium">{kpi.title}</span>
                    <Icon className={`w-4 h-4 ${kpi.color}`} />
                  </div>
                  <div className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
                    {kpi.value}
                  </div>
                  <div className="text-[10px] text-emerald-600 dark:text-emerald-400 font-semibold mt-0.5">
                    {kpi.change}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
            {/* Monthly Hours Chart */}
            <Card className="lg:col-span-2 border-neutral-200 dark:border-[#262626]">
              <CardHeader className="pb-2 flex flex-row items-center justify-between">
                <CardTitle className="text-xs font-bold uppercase tracking-wider text-neutral-500">
                  Monthly Workforce Utilization
                </CardTitle>
                <Badge variant="default" size="sm">
                  Q2–Q3 2026
                </Badge>
              </CardHeader>
              <CardContent className="pt-2">
                <SimpleBarChart
                  data={[
                    { label: 'Apr', value: 5800 },
                    { label: 'May', value: 6100 },
                    { label: 'Jun', value: 5950 },
                    { label: 'Jul', value: 6300 },
                    { label: 'Aug', value: 6250 },
                    { label: 'Sep', value: 6420 },
                  ]}
                  height={150}
                  valueFormatter={(v) => `${v}h`}
                />
              </CardContent>
            </Card>

            {/* Attendance Donut Chart */}
            <Card className="border-neutral-200 dark:border-[#262626]">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-bold uppercase tracking-wider text-neutral-500">
                  Daily Attendance Split
                </CardTitle>
              </CardHeader>
              <CardContent className="flex flex-col items-center justify-center pt-2">
                <SimpleDonutChart
                  segments={[
                    { label: 'Present', value: 44, color: '#16a34a', darkColor: '#22c55e' },
                    { label: 'Leave', value: 3, color: '#d97706', darkColor: '#f59e0b' },
                    { label: 'Probation', value: 1, color: '#71717a', darkColor: '#a1a1aa' },
                  ]}
                  centerText="91.6%"
                  centerSubtext="Attendance"
                  size={140}
                />
              </CardContent>
            </Card>
          </div>

          {/* Operational Workflow Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-1">
            {/* Tasks Preview */}
            <div className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-neutral-900 dark:text-neutral-100 flex items-center gap-1.5">
                  <CheckSquare className="w-3.5 h-3.5 text-blue-600 dark:text-blue-400" />
                  Active Operational Tasks
                </span>
                <span className="text-[10px] text-neutral-400 font-mono">4 items</span>
              </div>
              <div className="space-y-2">
                {[
                  { title: 'Migrate customer schema to Postgres 16', status: 'IN_PROGRESS', tag: 'Engineering' },
                  { title: 'Prepare Q3 payroll allocation report', status: 'REVIEW', tag: 'Finance' },
                  { title: 'Staff rotation schedule onboarding', status: 'COMPLETED', tag: 'HRM' },
                ].map((task) => (
                  <div
                    key={task.title}
                    className="flex items-center justify-between p-2 rounded-lg bg-white dark:bg-[#1a1a1a] border border-neutral-100 dark:border-[#262626] text-xs"
                  >
                    <div className="flex items-center gap-2 truncate">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                      <span className="truncate text-neutral-800 dark:text-neutral-200">{task.title}</span>
                    </div>
                    <Badge variant="default" size="sm">
                      {task.status}
                    </Badge>
                  </div>
                ))}
              </div>
            </div>

            {/* Claims & Reimbursements Preview */}
            <div className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-neutral-900 dark:text-neutral-100 flex items-center gap-1.5">
                  <FileStack className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
                  Recent Expense Claims
                </span>
                <span className="text-[10px] text-neutral-400 font-mono">Automated workflow</span>
              </div>
              <div className="space-y-2">
                {[
                  { title: 'AWS Cloud Infrastructure Hosting', amount: '$1,250.00', status: 'APPROVED' },
                  { title: 'Client Onsite Travel & Lodging', amount: '$420.00', status: 'PENDING' },
                  { title: 'Software Team Annual Licenses', amount: '$3,150.00', status: 'APPROVED' },
                ].map((claim) => (
                  <div
                    key={claim.title}
                    className="flex items-center justify-between p-2 rounded-lg bg-white dark:bg-[#1a1a1a] border border-neutral-100 dark:border-[#262626] text-xs"
                  >
                    <div className="flex items-center gap-2 truncate">
                      <Calendar className="w-3.5 h-3.5 text-neutral-400 shrink-0" />
                      <span className="truncate text-neutral-800 dark:text-neutral-200">{claim.title}</span>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className="font-semibold font-mono text-neutral-900 dark:text-neutral-100">
                        {claim.amount}
                      </span>
                      <Badge
                        variant={claim.status === 'APPROVED' ? 'success' : 'warning'}
                        size="sm"
                      >
                        {claim.status}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
