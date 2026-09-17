import React from 'react';
import { Layers, EyeOff, Sliders, CheckCircle2, ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export const ProblemSolutionSection: React.FC = () => {
  const problems = [
    {
      number: '01',
      icon: Layers,
      title: 'Fragmented Workflows',
      description:
        'People data, project deliverables, billable hours, and employee expenses are dispersed across separate disconnected subscriptions.',
      symptom: 'Duplicate entries and synchronization delays',
    },
    {
      number: '02',
      icon: EyeOff,
      title: 'Limited Operational Visibility',
      description:
        'Leadership and managers lack a real-time, consolidated picture of employee availability, active project velocity, and claims spend.',
      symptom: 'Siloed reporting with stale data spreadsheets',
    },
    {
      number: '03',
      icon: Sliders,
      title: 'Administrative Complexity',
      description:
        'Onboarding new personnel, administering role permissions, adjusting schedules, and tracking leave approvals consumes excessive overhead.',
      symptom: 'Security vulnerabilities and administrative fatigue',
    },
  ];

  const solutions = [
    'Single tenant schema guarantees strict data isolation and zero cross-tenant leaks.',
    'Hierarchical RBAC (Super Admin, Admin, Manager, User) enforces least-privilege security.',
    'Integrated workforce lifecycle: attendance, time logs, tasks, and claims directly connected.',
    'Consolidated operational analytics with exportable reports and real-time dashboard widgets.',
  ];

  return (
    <section className="py-24 bg-neutral-50 dark:bg-[#0d0d0d] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-200/70 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-300/80 dark:border-[#262626] mb-4">
            The Architecture Challenge
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Your organization’s work shouldn’t live in disconnected systems.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            When teams rely on separate point solutions for HR, projects, timesheets, and claims,
            operational friction compounds with every new hire.
          </p>
        </div>

        {/* Problem Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-16">
          {problems.map((problem) => {
            const Icon = problem.icon;
            return (
              <div
                key={problem.number}
                className="relative p-6 rounded-xl bg-white dark:bg-[#141414] border border-neutral-200 dark:border-[#262626] shadow-xs flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-5">
                    <span className="text-xs font-mono font-bold text-neutral-400 dark:text-neutral-500">
                      {problem.number}
                    </span>
                    <div className="p-2 rounded-lg bg-neutral-100 dark:bg-[#1c1c1c] text-neutral-700 dark:text-neutral-300">
                      <Icon className="w-4 h-4" />
                    </div>
                  </div>

                  <h3 className="text-base font-semibold text-neutral-900 dark:text-neutral-100 mb-2">
                    {problem.title}
                  </h3>
                  <p className="text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed mb-4">
                    {problem.description}
                  </p>
                </div>

                <div className="pt-3 border-t border-neutral-100 dark:border-[#222]">
                  <span className="text-xs text-neutral-500 dark:text-neutral-400 font-medium italic">
                    Cost: {problem.symptom}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Consolidation Banner */}
        <div className="p-8 sm:p-10 rounded-2xl bg-white dark:bg-[#121212] border border-neutral-200 dark:border-[#262626] shadow-xs">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
            <div className="lg:col-span-5">
              <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded text-[11px] font-semibold uppercase tracking-wider bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800/60 mb-3">
                The Solution
              </div>
              <h3 className="text-2xl sm:text-3xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
                One platform.
                <br />
                One workspace.
                <br />
                One source of truth.
              </h3>
              <p className="mt-3 text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                Consolidate your entire business infrastructure into a multi-tenant platform designed for
                performance, compliance, and effortless role-based collaboration.
              </p>
              <div className="mt-6">
                <Link
                  to="/signup"
                  className="inline-flex items-center gap-2 text-xs font-semibold px-4 py-2 rounded-lg bg-neutral-900 text-white dark:bg-white dark:text-neutral-950 hover:opacity-90 transition-opacity"
                >
                  Create your workspace <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>

            <div className="lg:col-span-7 grid grid-cols-1 sm:grid-cols-2 gap-4">
              {solutions.map((item, idx) => (
                <div
                  key={idx}
                  className="p-4 rounded-xl bg-neutral-50 dark:bg-[#181818] border border-neutral-200/80 dark:border-[#262626] flex items-start gap-3"
                >
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
                  <span className="text-xs text-neutral-700 dark:text-neutral-300 font-medium leading-relaxed">
                    {item}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
