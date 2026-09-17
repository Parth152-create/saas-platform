import React from 'react';
import { Users, Calendar, Clock, Briefcase, FileCheck, ShieldCheck } from 'lucide-react';

interface SanitizedEmployee {
  name: string;
  role: string;
  department: string;
  status: 'Active' | 'On Leave' | 'Remote';
  shift: string;
  avatar: string;
}

const DEMO_EMPLOYEES: SanitizedEmployee[] = [
  {
    name: 'Elena Rostova',
    role: 'Principal Engineer',
    department: 'Engineering',
    status: 'Active',
    shift: 'Standard 09:00 - 17:00',
    avatar: 'ER',
  },
  {
    name: 'Marcus Vance',
    role: 'Director of Operations',
    department: 'Operations',
    status: 'Active',
    shift: 'Standard 09:00 - 17:00',
    avatar: 'MV',
  },
  {
    name: 'Sarah Chen',
    role: 'Senior Financial Analyst',
    department: 'Finance',
    status: 'On Leave',
    shift: 'Flexible Schedule',
    avatar: 'SC',
  },
  {
    name: 'David Kim',
    role: 'Product Designer',
    department: 'Design',
    status: 'Remote',
    shift: 'Standard 09:00 - 17:00',
    avatar: 'DK',
  },
];

const WORKFORCE_PILLARS = [
  {
    icon: Users,
    title: 'Employee Profiles',
    description: 'Centralized directory with contact records, roles, departments, and hire dates.',
  },
  {
    icon: Briefcase,
    title: 'Teams & Departments',
    description: 'Map departmental structures, assign department leads, and group workforce units.',
  },
  {
    icon: Clock,
    title: 'Attendance & Clock-In',
    description: 'Real-time daily attendance records, clock in/out stamps, and hours validation.',
  },
  {
    icon: Calendar,
    title: 'Leave Approvals',
    description: 'Multi-category leave requests, remaining balance deductions, and manager workflows.',
  },
  {
    icon: FileCheck,
    title: 'Work Schedules',
    description: 'Defined work shifts, weekly operational hours, and scheduling compliance.',
  },
  {
    icon: ShieldCheck,
    title: 'Personnel Documents',
    description: 'Encrypted document records, agreements, and compliance files per employee.',
  },
];

export const WorkforceSection: React.FC = () => {
  return (
    <section id="features" className="py-24 bg-white dark:bg-[#0a0a0a] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="max-w-3xl mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-100 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-200 dark:border-[#262626] mb-4">
            Human Resource Management
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Know your people. Empower your teams.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            A cohesive workforce record system giving managers and human resource administrators
            complete visibility into organizational structure, presence, and availability.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-start">
          {/* Left: Sanitized UI Table Preview */}
          <div className="lg:col-span-7">
            <div className="rounded-xl border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#111] overflow-hidden shadow-xs">
              <div className="px-5 py-4 border-b border-neutral-200 dark:border-[#262626] flex items-center justify-between bg-neutral-50/70 dark:bg-[#141414]">
                <div>
                  <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100">
                    Personnel Directory
                  </h3>
                  <p className="text-[11px] text-neutral-500 dark:text-neutral-400">
                    Live tenant workforce state &bull; Sanitized preview
                  </p>
                </div>
                <span className="text-xs font-mono px-2.5 py-0.5 rounded-full bg-neutral-200/70 dark:bg-[#222] text-neutral-700 dark:text-neutral-300 font-medium">
                  4 Active Records
                </span>
              </div>

              {/* Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-neutral-200 dark:border-[#262626] text-neutral-500 dark:text-neutral-400 font-medium uppercase text-[10px] tracking-wider">
                      <th className="py-3 px-5">Employee</th>
                      <th className="py-3 px-4">Department</th>
                      <th className="py-3 px-4">Shift Schedule</th>
                      <th className="py-3 px-5 text-right">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#1f1f1f]">
                    {DEMO_EMPLOYEES.map((emp) => (
                      <tr
                        key={emp.name}
                        className="hover:bg-neutral-50/50 dark:hover:bg-[#171717] transition-colors"
                      >
                        <td className="py-3 px-5">
                          <div className="flex items-center gap-3">
                            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-neutral-200 dark:bg-[#222] font-semibold text-[11px] text-neutral-800 dark:text-neutral-200">
                              {emp.avatar}
                            </div>
                            <div>
                              <div className="font-semibold text-neutral-900 dark:text-neutral-100">
                                {emp.name}
                              </div>
                              <div className="text-[11px] text-neutral-500 dark:text-neutral-400">
                                {emp.role}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <span className="inline-block px-2 py-0.5 rounded text-[11px] font-medium bg-neutral-100 dark:bg-[#1a1a1a] text-neutral-700 dark:text-neutral-300 border border-neutral-200 dark:border-[#2a2a2a]">
                            {emp.department}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-neutral-600 dark:text-neutral-400 font-mono text-[11px]">
                          {emp.shift}
                        </td>
                        <td className="py-3 px-5 text-right">
                          <span
                            className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wider ${
                              emp.status === 'Active'
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                                : emp.status === 'On Leave'
                                ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
                                : 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300'
                            }`}
                          >
                            {emp.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="px-5 py-3 border-t border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] flex items-center justify-between text-[11px] text-neutral-500 dark:text-neutral-400">
                <span>Schema-isolated employee records</span>
                <span>Protected by role authorization</span>
              </div>
            </div>
          </div>

          {/* Right: Feature Pillars */}
          <div className="lg:col-span-5 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-1 gap-4">
            {WORKFORCE_PILLARS.map((pillar) => {
              const Icon = pillar.icon;
              return (
                <div
                  key={pillar.title}
                  className="p-4 rounded-xl border border-neutral-200 dark:border-[#262626] bg-neutral-50/60 dark:bg-[#121212] flex items-start gap-3.5"
                >
                  <div className="p-2 rounded-lg bg-white dark:bg-[#1a1a1a] border border-neutral-200 dark:border-[#262626] text-neutral-900 dark:text-neutral-100 shrink-0">
                    <Icon className="w-4 h-4" />
                  </div>
                  <div>
                    <h4 className="text-xs font-semibold text-neutral-900 dark:text-neutral-100">
                      {pillar.title}
                    </h4>
                    <p className="mt-1 text-[11px] text-neutral-600 dark:text-neutral-400 leading-relaxed">
                      {pillar.description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
};
