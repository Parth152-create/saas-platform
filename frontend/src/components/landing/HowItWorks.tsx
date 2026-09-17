import React from 'react';
import { Building2, UserPlus, SlidersHorizontal, Rocket } from 'lucide-react';

export const HowItWorks: React.FC = () => {
  const steps = [
    {
      step: '01',
      icon: Building2,
      title: 'Create your workspace',
      description:
        'Register your organization with a unique tenant slug. A dedicated PostgreSQL schema is provisioned automatically with zero downtime.',
    },
    {
      step: '02',
      icon: UserPlus,
      title: 'Invite your team',
      description:
        'Send secure email invitations with preset roles (Admin, Manager, User). Invited members accept via cryptographic single-use tokens.',
    },
    {
      step: '03',
      icon: SlidersHorizontal,
      title: 'Configure roles & workflows',
      description:
        'Set up departments, assign leadership positions, configure work shifts, and define project boards tailored to your operational structure.',
    },
    {
      step: '04',
      icon: Rocket,
      title: 'Run your organization',
      description:
        'Clock attendance, log project hours, review expense claims, approve leave requests, and monitor cross-department operational analytics.',
    },
  ];

  return (
    <section id="how-it-works" className="py-24 bg-neutral-50 dark:bg-[#0d0d0d] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-200/70 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-300/80 dark:border-[#262626] mb-4">
            Simple Onboarding
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Get your organization up and running.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            From sign-up to full workforce deployment in minutes. No complex enterprise sales cycles
            or lengthy multi-week configurations.
          </p>
        </div>

        {/* Steps Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 relative">
          {steps.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.step}
                className="relative p-6 rounded-xl bg-white dark:bg-[#141414] border border-neutral-200 dark:border-[#262626] shadow-xs flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <span className="text-xs font-mono font-bold text-neutral-400 dark:text-neutral-500">
                      STEP {item.step}
                    </span>
                    <div className="p-2 rounded-lg bg-neutral-100 dark:bg-[#1f1f1f] text-neutral-800 dark:text-neutral-200 border border-neutral-200 dark:border-[#262626]">
                      <Icon className="w-4 h-4" />
                    </div>
                  </div>
                  <h3 className="text-base font-semibold text-neutral-900 dark:text-neutral-100 mb-2">
                    {item.title}
                  </h3>
                  <p className="text-xs text-neutral-600 dark:text-neutral-400 leading-relaxed">
                    {item.description}
                  </p>
                </div>

                <div className="mt-6 pt-4 border-t border-neutral-100 dark:border-[#222]">
                  <div className="h-1 w-8 rounded-full bg-neutral-900 dark:bg-neutral-100" />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};
