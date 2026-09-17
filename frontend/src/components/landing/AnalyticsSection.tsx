import React from 'react';
import { SimpleBarChart, SimpleDonutChart } from '../widgets/SimpleChart';
import { BarChart3, TrendingUp, Download, Check } from 'lucide-react';

export const AnalyticsSection: React.FC = () => {
  // Sanitized demo charts
  const departmentUtilization = [
    { label: 'Engineering', value: 420 },
    { label: 'Operations', value: 310 },
    { label: 'Design', value: 190 },
    { label: 'Finance', value: 140 },
    { label: 'Marketing', value: 110 },
  ];

  const workforceAllocation = [
    { label: 'Billable Client Projects', value: 65, color: '#3b82f6' },
    { label: 'Internal Operations', value: 25, color: '#10b981' },
    { label: 'R&D / Training', value: 10, color: '#8b5cf6' },
  ];

  return (
    <section className="py-24 bg-white dark:bg-[#0a0a0a] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="max-w-3xl mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-100 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-200 dark:border-[#262626] mb-4">
            Intelligence & Visibility
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Turn operational activity into visibility.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            Real-time analytics transform everyday work logs, attendance stamps, and expense claims
            into clear operational dashboards and executive reports.
          </p>
        </div>

        {/* Charts & Tiers Comparison */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Chart Visualizations Preview */}
          <div className="lg:col-span-7 space-y-6">
            <div className="p-6 rounded-xl bg-neutral-50 dark:bg-[#121212] border border-neutral-200 dark:border-[#262626]">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                    Monthly Hours by Department
                  </h3>
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    Aggregated time logs &bull; Sanitized preview
                  </p>
                </div>
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-neutral-200/70 dark:bg-[#1f1f1f] text-neutral-700 dark:text-neutral-300">
                  1,170 hrs total
                </span>
              </div>
              <SimpleBarChart data={departmentUtilization} height={180} />
            </div>

            <div className="p-6 rounded-xl bg-neutral-50 dark:bg-[#121212] border border-neutral-200 dark:border-[#262626]">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                    Workforce Allocation Distribution
                  </h3>
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    Resource utilization split
                  </p>
                </div>
              </div>
              <SimpleDonutChart segments={workforceAllocation} size={170} />
            </div>
          </div>

          {/* Tier Comparison Breakdown */}
          <div className="lg:col-span-5 space-y-4">
            {/* Basic Reports Card */}
            <div className="p-6 rounded-xl bg-neutral-50 dark:bg-[#121212] border border-neutral-200 dark:border-[#262626]">
              <div className="flex items-center gap-2 mb-2">
                <span className="p-1.5 rounded-md bg-neutral-200/70 dark:bg-[#222] text-neutral-800 dark:text-neutral-200">
                  <BarChart3 className="w-4 h-4" />
                </span>
                <span className="text-xs font-mono font-semibold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">
                  Included in Starter
                </span>
              </div>
              <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                Basic Operational Reports
              </h3>
              <p className="text-xs text-neutral-600 dark:text-neutral-400 mt-1 mb-4 leading-relaxed">
                Standard reporting essential for daily workforce and project management.
              </p>
              <ul className="space-y-2 text-xs text-neutral-700 dark:text-neutral-300">
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0" />
                  Employee attendance & shift records
                </li>
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0" />
                  Task completion & milestone logs
                </li>
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0" />
                  Standard timesheets & project hours
                </li>
              </ul>
            </div>

            {/* Advanced Reports & Analytics Card */}
            <div className="p-6 rounded-xl bg-neutral-950 text-white dark:bg-[#141414] dark:text-neutral-100 border border-neutral-800 dark:border-[#333] shadow-md">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="p-1.5 rounded-md bg-neutral-800 dark:bg-[#222] text-white">
                    <TrendingUp className="w-4 h-4" />
                  </span>
                  <span className="text-xs font-mono font-semibold uppercase tracking-wider text-neutral-400">
                    Pro & Enterprise
                  </span>
                </div>
                <span className="text-[10px] font-semibold uppercase px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 border border-blue-500/30">
                  Advanced
                </span>
              </div>
              <h3 className="text-base font-bold">
                Advanced Reports & Analytics
              </h3>
              <p className="text-xs text-neutral-400 mt-1 mb-4 leading-relaxed">
                Deep analytical capabilities for department heads and executive leadership.
              </p>
              <ul className="space-y-2 text-xs text-neutral-300">
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  Departmental cost & claim allocation analysis
                </li>
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  Workforce capacity & utilization trends
                </li>
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  Exportable executive reports (CSV, PDF, JSON)
                </li>
                <li className="flex items-center gap-2">
                  <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  Custom time frame comparative metrics
                </li>
              </ul>
              <div className="mt-5 pt-4 border-t border-neutral-800 dark:border-[#2a2a2a] flex items-center gap-2 text-neutral-400 text-xs">
                <Download className="w-3.5 h-3.5" />
                <span>One-click exports ready for audits and payroll</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
