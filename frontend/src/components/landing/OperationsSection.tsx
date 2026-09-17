import React, { useState } from 'react';
import {
  FolderGit2,
  CheckSquare,
  Receipt,
  Clock,
  BarChart3,
  ArrowRight,
  Check,
} from 'lucide-react';

interface Stage {
  id: string;
  name: string;
  status: 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'COMPLETED';
  taskTitle: string;
  assignee: string;
  hoursLogged: string;
  claimAmount?: string;
}

const DEMO_STAGES: Stage[] = [
  {
    id: '1',
    name: '01 Planning',
    status: 'COMPLETED',
    taskTitle: 'Q2 Infrastructure Architecture Specs',
    assignee: 'Elena Rostova',
    hoursLogged: '14.5 hrs',
  },
  {
    id: '2',
    name: '02 Execution',
    status: 'IN_PROGRESS',
    taskTitle: 'PostgreSQL Multi-Tenant Migration',
    assignee: 'Marcus Vance',
    hoursLogged: '28.0 hrs',
    claimAmount: '$240.00 Server Ops',
  },
  {
    id: '3',
    name: '03 Review',
    status: 'REVIEW',
    taskTitle: 'Security Audit & RBAC Verification',
    assignee: 'Sarah Chen',
    hoursLogged: '8.5 hrs',
  },
  {
    id: '4',
    name: '04 Delivery',
    status: 'TODO',
    taskTitle: 'Staging Environment Rollout & Sign-off',
    assignee: 'David Kim',
    hoursLogged: '0.0 hrs',
  },
];

const PIPELINE_STEPS = [
  {
    step: '01',
    title: 'Projects',
    icon: FolderGit2,
    desc: 'Define scopes, budget boundaries, and assign team owners.',
  },
  {
    step: '02',
    title: 'Tasks',
    icon: CheckSquare,
    desc: 'Break objectives into actionable items with status stages.',
  },
  {
    step: '03',
    title: 'Time Logs',
    icon: Clock,
    desc: 'Team members record hours directly against active tasks.',
  },
  {
    step: '04',
    title: 'Claims',
    icon: Receipt,
    desc: 'Submit receipts and expenses tied directly to project codes.',
  },
  {
    step: '05',
    title: 'Reports',
    icon: BarChart3,
    desc: 'Instant operational visibility across hours, budget, and delivery.',
  },
];

export const OperationsSection: React.FC = () => {
  const [activeStage, setActiveStage] = useState<number>(1);

  return (
    <section className="py-24 bg-neutral-50 dark:bg-[#0d0d0d] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="max-w-3xl mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-200/70 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-300/80 dark:border-[#262626] mb-4">
            Operations & Project Lifecycle
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            From planning to completion.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            Work flows naturally across interconnected operational primitives. Projects break into
            tasks, tasks record time, claims attach to delivery, and reports synthesize progress.
          </p>
        </div>

        {/* The Connected Pipeline Chain */}
        <div className="mb-16">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
            {PIPELINE_STEPS.map((p, idx) => {
              const Icon = p.icon;
              const isLast = idx === PIPELINE_STEPS.length - 1;
              return (
                <div
                  key={p.title}
                  className="relative p-5 rounded-xl bg-white dark:bg-[#141414] border border-neutral-200 dark:border-[#262626] shadow-xs flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-[11px] font-mono font-bold text-neutral-400 dark:text-neutral-500">
                        {p.step}
                      </span>
                      <div className="p-1.5 rounded-md bg-neutral-100 dark:bg-[#1f1f1f] text-neutral-800 dark:text-neutral-200">
                        <Icon className="w-3.5 h-3.5" />
                      </div>
                    </div>
                    <h3 className="text-sm font-semibold text-neutral-900 dark:text-neutral-100 mb-1">
                      {p.title}
                    </h3>
                    <p className="text-xs text-neutral-600 dark:text-neutral-400 leading-relaxed">
                      {p.desc}
                    </p>
                  </div>
                  {!isLast && (
                    <div className="hidden lg:block absolute -right-2 top-1/2 -translate-y-1/2 z-10">
                      <div className="w-4 h-4 rounded-full bg-neutral-200 dark:bg-[#262626] flex items-center justify-center text-neutral-600 dark:text-neutral-300">
                        <ArrowRight className="w-2.5 h-2.5" />
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Visual Lifecycle Stepper Mockup */}
        <div className="rounded-2xl border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#121212] p-6 sm:p-8 shadow-xs">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-neutral-200 dark:border-[#262626]">
            <div>
              <span className="text-xs font-mono uppercase tracking-wider text-neutral-500 dark:text-neutral-400 font-medium">
                Live Workflow Simulation
              </span>
              <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">
                Core Initiative: Enterprise Data Platform Q2
              </h3>
            </div>
            {/* Status pills selector */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
              {DEMO_STAGES.map((stg, i) => (
                <button
                  key={stg.id}
                  type="button"
                  onClick={() => setActiveStage(i)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap cursor-pointer ${
                    activeStage === i
                      ? 'bg-neutral-900 text-white dark:bg-white dark:text-neutral-900 font-semibold'
                      : 'bg-neutral-100 dark:bg-[#1c1c1c] text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-200'
                  }`}
                >
                  {stg.name}
                </button>
              ))}
            </div>
          </div>

          {/* Active Stage Card */}
          <div className="mt-6 p-6 rounded-xl bg-neutral-50 dark:bg-[#171717] border border-neutral-200/80 dark:border-[#262626]">
            <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
              <div className="md:col-span-7">
                <div className="flex items-center gap-2 mb-2">
                  <span
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-semibold tracking-wider uppercase ${
                      DEMO_STAGES[activeStage].status === 'COMPLETED'
                        ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                        : DEMO_STAGES[activeStage].status === 'IN_PROGRESS'
                        ? 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300'
                        : DEMO_STAGES[activeStage].status === 'REVIEW'
                        ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
                        : 'bg-neutral-200 text-neutral-800 dark:bg-[#262626] dark:text-neutral-300'
                    }`}
                  >
                    {DEMO_STAGES[activeStage].status === 'COMPLETED' && (
                      <Check className="w-3 h-3" />
                    )}
                    {DEMO_STAGES[activeStage].status}
                  </span>
                  <span className="text-xs text-neutral-500 dark:text-neutral-400 font-mono">
                    STAGE {activeStage + 1} OF 4
                  </span>
                </div>
                <h4 className="text-lg font-bold text-neutral-900 dark:text-neutral-100">
                  {DEMO_STAGES[activeStage].taskTitle}
                </h4>
                <p className="text-xs text-neutral-600 dark:text-neutral-400 mt-2">
                  Assigned to <span className="font-semibold text-neutral-900 dark:text-neutral-200">{DEMO_STAGES[activeStage].assignee}</span>.
                  All activities, billable hours, and related expense claims update automatically.
                </p>
              </div>

              <div className="md:col-span-5 grid grid-cols-2 gap-3">
                <div className="p-3.5 rounded-lg bg-white dark:bg-[#121212] border border-neutral-200 dark:border-[#262626]">
                  <div className="text-[11px] font-medium text-neutral-500 dark:text-neutral-400">
                    Logged Hours
                  </div>
                  <div className="text-base font-bold font-mono text-neutral-900 dark:text-neutral-100 mt-1">
                    {DEMO_STAGES[activeStage].hoursLogged}
                  </div>
                </div>

                <div className="p-3.5 rounded-lg bg-white dark:bg-[#121212] border border-neutral-200 dark:border-[#262626]">
                  <div className="text-[11px] font-medium text-neutral-500 dark:text-neutral-400">
                    Linked Claim
                  </div>
                  <div className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 mt-1.5 truncate">
                    {DEMO_STAGES[activeStage].claimAmount || 'None'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
