import React from 'react';
import { Link } from 'react-router-dom';
import { Check, ArrowRight } from 'lucide-react';
import { Button } from '../common/Button';
import { PricingComparison } from './PricingComparison';

interface PlanTierCard {
  id: string;
  name: string;
  price: string;
  frequency: string;
  description: string;
  popular?: boolean;
  features: string[];
  ctaText: string;
  href: string;
}

const PLANS: PlanTierCard[] = [
  {
    id: 'starter',
    name: 'Starter',
    price: '$0',
    frequency: 'forever',
    description: 'Essential workforce, project execution, and time tracking for emerging teams.',
    features: [
      'Employee Directory & Department Structure',
      'Projects & Stage-based Task Boards',
      'Daily Attendance & Work Shifts',
      'Time Tracking & Timesheet Logging',
      'Leave Management & Approvals',
      'Claims & Expense Submissions',
      'Basic Operational Reports',
    ],
    ctaText: 'Start Free Workspace',
    href: '/signup?plan=starter',
  },
  {
    id: 'pro',
    name: 'Pro',
    price: '$49',
    frequency: '/ month',
    popular: true,
    description: 'Deep operational visibility, advanced analytics, and expanded HRM for growing companies.',
    features: [
      'Everything in Starter',
      'Advanced Reports & Data Exports',
      'Advanced Analytics & Capacity Trends',
      'Advanced HRM Organizational Architecture',
      'Cross-department Utilization Tracking',
      'Automated Expense Auditing',
      'Priority System Support',
    ],
    ctaText: 'Get Started with Pro',
    href: '/signup?plan=pro',
  },
  {
    id: 'enterprise',
    name: 'Enterprise',
    price: '$199',
    frequency: '/ month',
    description: 'Custom lifecycle workflows, webhook integrations, and dedicated tenant governance.',
    features: [
      'Everything in Pro',
      'Custom Workflows & State Progression',
      'Advanced Integrations & Webhook Subscriptions',
      'Schema-per-Tenant PostgreSQL Data Isolation',
      'Cryptographic JWT + Refresh Token Rotation',
      'Full Operational Audit Trails',
      'Enterprise SLA Support',
    ],
    ctaText: 'Deploy Enterprise',
    href: '/signup?plan=enterprise',
  },
];

export const PricingSection: React.FC = () => {
  return (
    <section id="pricing" className="py-24 bg-white dark:bg-[#0a0a0a] border-b border-neutral-200 dark:border-[#262626]">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-16">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold tracking-wide uppercase bg-neutral-100 text-neutral-800 dark:bg-[#1a1a1a] dark:text-neutral-300 border border-neutral-200 dark:border-[#262626] mb-4">
            Transparent Pricing
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 leading-tight">
            Plans that scale with your business.
          </h2>
          <p className="mt-4 text-base sm:text-lg text-neutral-600 dark:text-neutral-400">
            Choose the capabilities your organization needs today, with room to expand as your team
            grows. Powered by Stripe billing.
          </p>
        </div>

        {/* 3 Tier Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 items-stretch">
          {PLANS.map((plan) => (
            <div
              key={plan.id}
              className={`relative flex flex-col justify-between rounded-2xl p-8 transition-all duration-300 ${
                plan.popular
                  ? 'bg-neutral-950 text-white dark:bg-[#141414] dark:text-neutral-100 border-2 border-neutral-900 dark:border-neutral-100 shadow-xl scale-[1.02]'
                  : 'bg-neutral-50/80 dark:bg-[#111] text-neutral-900 dark:text-neutral-100 border border-neutral-200 dark:border-[#262626] shadow-xs'
              }`}
            >
              {plan.popular && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider bg-neutral-900 text-white dark:bg-white dark:text-neutral-950 border border-neutral-700 dark:border-neutral-300">
                  Most Popular
                </div>
              )}

              <div>
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-bold">{plan.name}</h3>
                </div>

                <p
                  className={`mt-2 text-xs leading-relaxed ${
                    plan.popular ? 'text-neutral-300' : 'text-neutral-600 dark:text-neutral-400'
                  }`}
                >
                  {plan.description}
                </p>

                <div className="mt-6 flex items-baseline gap-1">
                  <span className="text-4xl font-extrabold tracking-tight">{plan.price}</span>
                  <span
                    className={`text-xs font-mono ${
                      plan.popular ? 'text-neutral-400' : 'text-neutral-500 dark:text-neutral-400'
                    }`}
                  >
                    {plan.frequency}
                  </span>
                </div>

                {/* Features list */}
                <div className="mt-8 pt-6 border-t border-neutral-200/60 dark:border-[#262626]">
                  <p
                    className={`text-[11px] font-bold uppercase tracking-wider font-mono mb-4 ${
                      plan.popular ? 'text-neutral-300' : 'text-neutral-500 dark:text-neutral-400'
                    }`}
                  >
                    Included Capabilities
                  </p>
                  <ul className="space-y-3">
                    {plan.features.map((feat) => (
                      <li key={feat} className="flex items-start gap-2.5 text-xs">
                        <Check
                          className={`w-4 h-4 shrink-0 mt-0.5 ${
                            plan.popular
                              ? 'text-emerald-400'
                              : 'text-emerald-600 dark:text-emerald-400'
                          }`}
                        />
                        <span
                          className={
                            plan.popular
                              ? 'text-neutral-200'
                              : 'text-neutral-700 dark:text-neutral-300'
                          }
                        >
                          {feat}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>

              {/* Action */}
              <div className="mt-8 pt-6">
                <Link to={plan.href} className="w-full block">
                  <Button
                    className="w-full justify-center"
                    variant={plan.popular ? 'secondary' : 'primary'}
                    size="md"
                    rightIcon={<ArrowRight className="w-4 h-4" />}
                  >
                    {plan.ctaText}
                  </Button>
                </Link>
              </div>
            </div>
          ))}
        </div>

        {/* Feature Entitlement Matrix & Architecture Deep Dive */}
        <PricingComparison />
      </div>
    </section>
  );
};
