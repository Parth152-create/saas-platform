import React, { useState } from 'react';
import { Check, Minus, ChevronDown, ChevronUp, Cpu, ArrowRight } from 'lucide-react';

interface FeatureRow {
  name: string;
  category: 'Core HRM' | 'Operations' | 'Analytics & Intelligence' | 'Enterprise';
  description: string;
  starter: boolean;
  pro: boolean;
  enterprise: boolean;
}

const FEATURE_MATRIX: FeatureRow[] = [
  // Core HRM
  {
    name: 'Employee Management',
    category: 'Core HRM',
    description: 'Centralized directory, employee profiles, and emergency contacts.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Teams & Departments',
    category: 'Core HRM',
    description: 'Hierarchical department units, team structures, and leadership roles.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Attendance Tracking',
    category: 'Core HRM',
    description: 'Daily clock-in/out timestamps and real-time workforce presence logs.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Leave Management',
    category: 'Core HRM',
    description: 'Annual, medical, and unpaid leave requests with approval workflows.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Work Schedules',
    category: 'Core HRM',
    description: 'Defined shift routines, weekly work hours, and scheduling allocations.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Document Records',
    category: 'Core HRM',
    description: 'Secure tenant-isolated storage for workforce documentation and files.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Advanced HRM',
    category: 'Core HRM',
    description: 'Expanded organizational hierarchy modeling and extended HR fields.',
    starter: false,
    pro: true,
    enterprise: true,
  },

  // Operations
  {
    name: 'Projects',
    category: 'Operations',
    description: 'Project scopes, milestone tracking, and cross-functional initiatives.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Tasks',
    category: 'Operations',
    description: 'Task board assignments, stage progression, and priority labels.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Time Tracking',
    category: 'Operations',
    description: 'Billable and non-billable task time logging and timesheet generation.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Claims & Expenses',
    category: 'Operations',
    description: 'Reimbursement submissions, category classifications, and receipts.',
    starter: true,
    pro: true,
    enterprise: true,
  },

  // Analytics & Intelligence
  {
    name: 'Basic Reports',
    category: 'Analytics & Intelligence',
    description: 'Standard workforce summaries, attendance logs, and task status lists.',
    starter: true,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Advanced Reports',
    category: 'Analytics & Intelligence',
    description: 'Cross-project cost allocations, department utilization, and data exports.',
    starter: false,
    pro: true,
    enterprise: true,
  },
  {
    name: 'Advanced Analytics',
    category: 'Analytics & Intelligence',
    description: 'Predictive utilization metrics, capacity forecasting, and deep insights.',
    starter: false,
    pro: true,
    enterprise: true,
  },

  // Enterprise
  {
    name: 'Custom Workflows',
    category: 'Enterprise',
    description: 'Tailored state machine transitions and multi-step approval pipelines.',
    starter: false,
    pro: false,
    enterprise: true,
  },
  {
    name: 'Advanced Integrations',
    category: 'Enterprise',
    description: 'Enterprise webhook hooks and programmatic API data pipeline access.',
    starter: false,
    pro: false,
    enterprise: true,
  },
];

export const PricingComparison: React.FC = () => {
  const [isExpanded, setIsExpanded] = useState<boolean>(false);

  const categories = Array.from(new Set(FEATURE_MATRIX.map((f) => f.category)));

  return (
    <div className="mt-16">
      {/* Feature Entitlement Architecture Explanation */}
      <div className="p-8 rounded-2xl bg-white dark:bg-[#121212] border border-neutral-200 dark:border-[#262626] mb-12 shadow-xs">
        <div className="max-w-3xl">
          <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded text-[11px] font-semibold uppercase tracking-wider bg-neutral-100 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-200 dark:border-[#262626] mb-3">
            <Cpu className="w-3.5 h-3.5" />
            Entitlement Architecture
          </div>
          <h3 className="text-xl sm:text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100">
            Your subscription controls your platform capabilities.
          </h3>
          <p className="mt-2 text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
            Each subscription tier maps directly to a cryptographically validated set of feature
            entitlements. Access is strictly enforced by the backend API aspect layer at runtime—not
            superficial client-side checks.
          </p>
        </div>

        {/* Entitlement Chain Flow Diagram */}
        <div className="mt-8 pt-6 border-t border-neutral-200 dark:border-[#262626]">
          <div className="flex flex-wrap items-center justify-between gap-3 text-center">
            <div className="flex-1 min-w-[120px] p-3 rounded-lg bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626]">
              <div className="text-[10px] uppercase font-mono text-neutral-500 dark:text-neutral-400 font-semibold">
                Step 1
              </div>
              <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">
                Active Tenant
              </div>
            </div>

            <ArrowRight className="w-4 h-4 text-neutral-400 shrink-0 hidden sm:block" />

            <div className="flex-1 min-w-[120px] p-3 rounded-lg bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626]">
              <div className="text-[10px] uppercase font-mono text-neutral-500 dark:text-neutral-400 font-semibold">
                Step 2
              </div>
              <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">
                Stripe Plan
              </div>
            </div>

            <ArrowRight className="w-4 h-4 text-neutral-400 shrink-0 hidden sm:block" />

            <div className="flex-1 min-w-[120px] p-3 rounded-lg bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626]">
              <div className="text-[10px] uppercase font-mono text-neutral-500 dark:text-neutral-400 font-semibold">
                Step 3
              </div>
              <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">
                Entitlements
              </div>
            </div>

            <ArrowRight className="w-4 h-4 text-neutral-400 shrink-0 hidden sm:block" />

            <div className="flex-1 min-w-[120px] p-3 rounded-lg bg-neutral-50 dark:bg-[#181818] border border-neutral-200 dark:border-[#262626]">
              <div className="text-[10px] uppercase font-mono text-neutral-500 dark:text-neutral-400 font-semibold">
                Step 4
              </div>
              <div className="text-xs font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">
                Backend AOP Guard
              </div>
            </div>

            <ArrowRight className="w-4 h-4 text-neutral-400 shrink-0 hidden sm:block" />

            <div className="flex-1 min-w-[120px] p-3 rounded-lg bg-neutral-900 text-white dark:bg-white dark:text-neutral-950 font-bold border border-neutral-900 dark:border-white">
              <div className="text-[10px] uppercase font-mono opacity-80 font-semibold">
                Step 5
              </div>
              <div className="text-xs mt-0.5">Unlocked Features</div>
            </div>
          </div>
        </div>
      </div>

      {/* Accordion / Table Header */}
      <div className="rounded-2xl border border-neutral-200 dark:border-[#262626] bg-white dark:bg-[#121212] overflow-hidden shadow-xs">
        <div
          onClick={() => setIsExpanded(!isExpanded)}
          className="px-6 py-4 flex items-center justify-between cursor-pointer hover:bg-neutral-50 dark:hover:bg-[#161616] transition-colors"
        >
          <div>
            <h4 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
              Detailed Feature Entitlement Matrix
            </h4>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
              Compare all 16 platform capabilities across Starter, Pro, and Enterprise tiers
            </p>
          </div>
          <button
            type="button"
            className="flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-lg border border-neutral-200 dark:border-[#262626] text-neutral-700 dark:text-neutral-300"
          >
            {isExpanded ? (
              <>
                Collapse Matrix <ChevronUp className="w-3.5 h-3.5" />
              </>
            ) : (
              <>
                Expand Matrix <ChevronDown className="w-3.5 h-3.5" />
              </>
            )}
          </button>
        </div>

        {/* Feature Comparison Table */}
        {isExpanded && (
          <div className="overflow-x-auto border-t border-neutral-200 dark:border-[#262626]">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-neutral-200 dark:border-[#262626] bg-neutral-50/80 dark:bg-[#141414] text-neutral-600 dark:text-neutral-400 font-medium">
                  <th className="py-3 px-6 w-2/5">Capability</th>
                  <th className="py-3 px-4 text-center w-1/5">Starter ($0)</th>
                  <th className="py-3 px-4 text-center w-1/5">Pro ($49/mo)</th>
                  <th className="py-3 px-4 text-center w-1/5">Enterprise ($199/mo)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#1c1c1c]">
                {categories.map((cat) => (
                  <React.Fragment key={cat}>
                    <tr className="bg-neutral-100/50 dark:bg-[#181818]">
                      <td
                        colSpan={4}
                        className="py-2 px-6 text-[10px] font-bold uppercase tracking-wider text-neutral-500 dark:text-neutral-400 font-mono"
                      >
                        {cat}
                      </td>
                    </tr>
                    {FEATURE_MATRIX.filter((f) => f.category === cat).map((f) => (
                      <tr
                        key={f.name}
                        className="hover:bg-neutral-50/50 dark:hover:bg-[#151515] transition-colors"
                      >
                        <td className="py-3 px-6">
                          <div className="font-semibold text-neutral-900 dark:text-neutral-100">
                            {f.name}
                          </div>
                          <div className="text-[11px] text-neutral-500 dark:text-neutral-400">
                            {f.description}
                          </div>
                        </td>
                        <td className="py-3 px-4 text-center">
                          {f.starter ? (
                            <div className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                              <Check className="w-3 h-3" />
                            </div>
                          ) : (
                            <Minus className="w-4 h-4 text-neutral-300 dark:text-neutral-600 mx-auto" />
                          )}
                        </td>
                        <td className="py-3 px-4 text-center">
                          {f.pro ? (
                            <div className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                              <Check className="w-3 h-3" />
                            </div>
                          ) : (
                            <Minus className="w-4 h-4 text-neutral-300 dark:text-neutral-600 mx-auto" />
                          )}
                        </td>
                        <td className="py-3 px-4 text-center">
                          {f.enterprise ? (
                            <div className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
                              <Check className="w-3 h-3" />
                            </div>
                          ) : (
                            <Minus className="w-4 h-4 text-neutral-300 dark:text-neutral-600 mx-auto" />
                          )}
                        </td>
                      </tr>
                    ))}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
