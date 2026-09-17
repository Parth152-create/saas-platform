import React, { useEffect, useState, useCallback } from 'react';
import {
  ArrowRight,
  Check,
  CreditCard,
  Download,
  ExternalLink,
  FileText,
  HelpCircle,
  Lock,
  RefreshCw,
  Shield,
  Sparkles,
  Zap,
} from 'lucide-react';
import { billingApi } from '../../api/billingApi';
import type { BillingSummaryResponse, Feature, PlanTier } from '../../api/types';
import { useAuth } from '../../context/AuthContext';
import { useEntitlements } from '../../context/EntitlementsContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { SkeletonCard, SkeletonTable } from '../../components/common/LoadingSkeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { formatCurrency, formatDate } from '../../utils/formatters';

interface FeatureMeta {
  key: Feature;
  label: string;
  description: string;
  category: 'Core Operations' | 'Intelligence & Analytics' | 'Enterprise & Workflows';
  tierRequired: 'STARTER' | 'PRO' | 'ENTERPRISE';
}

const FEATURE_CATALOG: FeatureMeta[] = [
  {
    key: 'EMPLOYEE_MANAGEMENT',
    label: 'Employee Management',
    description: 'Maintain employee directory, personal records, and employment status.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'TEAM_MANAGEMENT',
    label: 'Teams & Departments',
    description: 'Organize workforce into functional departments and reporting structures.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'PROJECT_MANAGEMENT',
    label: 'Project Tracking',
    description: 'Create client projects, assign teams, and monitor milestones.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'TASK_MANAGEMENT',
    label: 'Task Management',
    description: 'Kanban workflow boards with teammate assignments and status tracking.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'CLAIMS',
    label: 'Expense Claims',
    description: 'Submit, review, and approve staff expense reimbursement requests.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'TIME_TRACKING',
    label: 'Time Tracking',
    description: 'Log billable hours, project timesheets, and weekly work summaries.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'ATTENDANCE',
    label: 'Attendance Tracking',
    description: 'Daily clock-in/out records, timesheet compliance, and punctuality logs.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'LEAVE_MANAGEMENT',
    label: 'Leave Management',
    description: 'Track annual leave balances, sick leaves, and vacation requests.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'WORK_SCHEDULES',
    label: 'Work Schedules',
    description: 'Define organizational shift models, working hours, and schedules.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'DOCUMENTS',
    label: 'Documents & Files',
    description: 'Secure file storage for employment contracts and identity records.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'BASIC_REPORTS',
    label: 'Basic Reports',
    description: 'Standard operational exports for attendance logs and employee rosters.',
    category: 'Core Operations',
    tierRequired: 'STARTER',
  },
  {
    key: 'ADVANCED_REPORTS',
    label: 'Advanced Workforce Reports',
    description: 'Workforce cost allocation, utilization metrics, and PTO accrual forecasting.',
    category: 'Intelligence & Analytics',
    tierRequired: 'PRO',
  },
  {
    key: 'ADVANCED_ANALYTICS',
    label: 'Advanced Analytics',
    description: 'Headcount growth analytics, interactive charts, and department run rates.',
    category: 'Intelligence & Analytics',
    tierRequired: 'PRO',
  },
  {
    key: 'ADVANCED_HRM',
    label: 'Advanced HRM Modules',
    description: 'Comprehensive talent management, performance notes, and HR workflows.',
    category: 'Intelligence & Analytics',
    tierRequired: 'PRO',
  },
  {
    key: 'CUSTOM_WORKFLOWS',
    label: 'Custom Workflows',
    description: 'Configurable multi-level approval matrices and automated escalation paths.',
    category: 'Enterprise & Workflows',
    tierRequired: 'ENTERPRISE',
  },
  {
    key: 'ADVANCED_INTEGRATIONS',
    label: 'Advanced Integrations',
    description: 'Direct third-party payroll APIs, webhooks, and enterprise SSO enforcement.',
    category: 'Enterprise & Workflows',
    tierRequired: 'ENTERPRISE',
  },
];

export const SubscriptionSettingsPage: React.FC = () => {
  const { hasRole } = useAuth();
  const { hasFeature, refetchEntitlements } = useEntitlements();
  const { showToast } = useToast();

  const [summary, setSummary] = useState<BillingSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpgrading, setIsUpgrading] = useState<string | null>(null);
  const [isOpeningPortal, setIsOpeningPortal] = useState(false);

  const fetchBillingData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [billingData] = await Promise.all([
        billingApi.getBillingSummary().catch(() => null),
        refetchEntitlements(),
      ]);
      setSummary(billingData);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to retrieve subscription details';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, [refetchEntitlements]);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      billingApi.getBillingSummary().catch(() => null),
      refetchEntitlements().catch(() => {}),
    ])
      .then(([billingData]) => {
        if (isMounted) {
          setSummary(billingData);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to retrieve subscription details';
          setError(msg);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [refetchEntitlements]);

  const handleUpgrade = async (planTier: PlanTier) => {
    if (!hasRole('ADMIN')) {
      showToast('error', 'Unauthorized', 'Only organization administrators can modify subscriptions.');
      return;
    }

    setIsUpgrading(planTier);
    try {
      const res = await billingApi.createCheckoutSession({ planTier });
      if (res.checkoutUrl) {
        showToast('info', 'Redirecting to Stripe Checkout...', 'Opening secure payment gateway');
        window.location.href = res.checkoutUrl;
      } else {
        throw new Error('No checkout URL returned by billing service.');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Checkout initiation failed';
      showToast('error', 'Checkout Error', msg);
    } finally {
      setIsUpgrading(null);
    }
  };

  const handleOpenPortal = async () => {
    if (!hasRole('ADMIN')) {
      showToast('error', 'Unauthorized', 'Only organization administrators can access the billing portal.');
      return;
    }

    setIsOpeningPortal(true);
    try {
      const res = await billingApi.createPortalSession();
      if (res.url) {
        showToast('info', 'Opening Customer Portal', 'Redirecting to Stripe portal');
        window.location.href = res.url;
      } else {
        throw new Error('No portal URL returned.');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Customer portal initiation failed';
      showToast('error', 'Portal Error', msg);
    } finally {
      setIsOpeningPortal(false);
    }
  };

  const rawPlan = summary?.plan || 'STARTER';
  const currentPlan = rawPlan === 'FREE' ? 'STARTER' : rawPlan;
  const subscription = summary?.subscription;
  const invoices = summary?.invoices || [];

  return (
    <div className="space-y-8">
      {/* Top Banner / Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-2 border-b border-neutral-200 dark:border-[#262626]">
        <div>
          <h2 className="text-xl font-bold text-neutral-900 dark:text-neutral-100 flex items-center gap-2">
            <CreditCard className="w-5 h-5 text-neutral-700 dark:text-neutral-300" />
            Subscription & Feature Entitlements
          </h2>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
            Manage your workspace subscription tier, explore unlocked platform capabilities, and review invoices.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={fetchBillingData}
            isLoading={isLoading}
            leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
          >
            Refresh
          </Button>
          {subscription && hasRole('ADMIN') && (
            <Button
              variant="secondary"
              size="sm"
              onClick={handleOpenPortal}
              isLoading={isOpeningPortal}
              leftIcon={<ExternalLink className="w-3.5 h-3.5" />}
            >
              Customer Portal
            </Button>
          )}
        </div>
      </div>

      {error && (
        <ErrorAlert
          title="Subscription Service Notice"
          message={error}
          onRetry={fetchBillingData}
        />
      )}

      {isLoading ? (
        <div className="space-y-6">
          <SkeletonCard />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </div>
          <SkeletonTable rows={4} />
        </div>
      ) : (
        <>
          {/* Section 1: Current Subscription Summary Card */}
          <Card className="p-6 bg-white dark:bg-[#141414] border-neutral-200 dark:border-[#262626] shadow-xs">
            <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
              <div className="space-y-3">
                <div className="flex items-center gap-2.5">
                  <span className="text-xs font-bold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">
                    Active Plan Tier
                  </span>
                  <Badge variant="primary" size="md">
                    {currentPlan}
                  </Badge>
                  {subscription ? (
                    <Badge variant="success" size="sm" withDot>
                      {subscription.status}
                    </Badge>
                  ) : (
                    <Badge variant="default" size="sm">
                      Standard
                    </Badge>
                  )}
                </div>

                <h3 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {currentPlan === 'STARTER' && 'Starter Business Tier'}
                  {currentPlan === 'PRO' && 'Professional Enterprise Subscription'}
                  {currentPlan === 'ENTERPRISE' && 'Enterprise Organization Suite'}
                </h3>

                <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-xl leading-relaxed">
                  {subscription ? (
                    <>
                      Billing cycle renews on{' '}
                      <strong className="text-neutral-900 dark:text-neutral-200">
                        {formatDate(subscription.currentPeriodEnd)}
                      </strong>{' '}
                      (monthly billing).
                      {subscription.cancelAtPeriodEnd && (
                        <span className="text-amber-600 dark:text-amber-400 font-semibold ml-1 block mt-1">
                          Subscription is set to cancel at the end of the current period.
                        </span>
                      )}
                    </>
                  ) : (
                    'Your workspace is on the Starter tier with access to core workforce operations. Upgrade to Pro or Enterprise to unlock advanced analytics and custom workflows.'
                  )}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                {subscription ? (
                  hasRole('ADMIN') && (
                    <Button
                      onClick={handleOpenPortal}
                      isLoading={isOpeningPortal}
                      leftIcon={<ExternalLink className="w-4 h-4" />}
                    >
                      Manage in Customer Portal
                    </Button>
                  )
                ) : (
                  hasRole('ADMIN') && (
                    <div className="flex items-center gap-2">
                      <Button
                        onClick={() => handleUpgrade('PRO')}
                        isLoading={isUpgrading === 'PRO'}
                        leftIcon={<Zap className="w-4 h-4" />}
                      >
                        Upgrade to Pro ($49/mo)
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => handleUpgrade('ENTERPRISE')}
                        isLoading={isUpgrading === 'ENTERPRISE'}
                        leftIcon={<Sparkles className="w-4 h-4" />}
                      >
                        Enterprise ($199/mo)
                      </Button>
                    </div>
                  )
                )}
              </div>
            </div>
          </Card>

          {/* Section 2: How Feature Access Works (Spec §10) */}
          <Card className="p-6 bg-neutral-50 dark:bg-[#111111] border-neutral-200 dark:border-[#262626]">
            <div className="space-y-4">
              <div className="flex items-center gap-2 text-neutral-900 dark:text-neutral-100">
                <HelpCircle className="w-4 h-4 text-blue-600 dark:text-blue-400" />
                <h4 className="text-sm font-bold uppercase tracking-wider">How Feature Access Works</h4>
              </div>

              <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-3xl leading-relaxed">
                Your organization’s subscription determines which platform capabilities are available to your team.
                Each subscription plan is associated with a defined set of feature entitlements. When your subscription
                changes, the platform automatically updates the capabilities available to your workspace.
                Feature access is strictly enforced by the backend and reflected throughout the user interface.
              </p>

              {/* Architectural Pipeline Diagram */}
              <div className="grid grid-cols-1 sm:grid-cols-5 gap-2 pt-2">
                {[
                  { title: '1. Subscription', subtitle: 'Stripe Billing State' },
                  { title: '2. Plan', subtitle: 'Starter / Pro / Enterprise' },
                  { title: '3. Entitlements', subtitle: 'Platform Matrix' },
                  { title: '4. Enforcement', subtitle: 'Backend Auth Guard' },
                  { title: '5. Unlocked Features', subtitle: 'App UI & APIs' },
                ].map((step, idx) => (
                  <div
                    key={step.title}
                    className="flex flex-col items-center justify-center p-3 rounded-lg bg-white dark:bg-[#1a1a1a] border border-neutral-200 dark:border-[#2a2a2a] text-center shadow-2xs relative"
                  >
                    <span className="text-[11px] font-bold text-neutral-900 dark:text-neutral-100">
                      {step.title}
                    </span>
                    <span className="text-[10px] text-neutral-500 dark:text-neutral-400 mt-0.5">
                      {step.subtitle}
                    </span>
                    {idx < 4 && (
                      <div className="hidden sm:block absolute -right-2 top-1/2 -translate-y-1/2 z-10 text-neutral-300 dark:text-neutral-600">
                        <ArrowRight className="w-3.5 h-3.5" />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          </Card>

          {/* Section 3: Feature Access Matrix (Spec §9) */}
          <div className="space-y-4">
            <div>
              <h3 className="text-base font-bold text-neutral-900 dark:text-neutral-100 flex items-center gap-2">
                <Shield className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
                Feature Access & Capabilities
              </h3>
              <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
                Your current subscription determines which platform features are active for your workspace.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {FEATURE_CATALOG.map((item) => {
                const entitled = hasFeature(item.key);

                return (
                  <div
                    key={item.key}
                    className={`p-4 rounded-xl border transition-all flex flex-col justify-between ${
                      entitled
                        ? 'bg-white dark:bg-[#141414] border-neutral-200 dark:border-[#262626] shadow-2xs'
                        : 'bg-neutral-50/70 dark:bg-[#111111]/70 border-dashed border-neutral-300 dark:border-[#2d2d2d] opacity-90'
                    }`}
                  >
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-neutral-400">
                          {item.category}
                        </span>
                        {entitled ? (
                          <Badge variant="success" size="sm">
                            Included
                          </Badge>
                        ) : (
                          <Badge variant="warning" size="sm">
                            {item.tierRequired} Plan
                          </Badge>
                        )}
                      </div>

                      <div className="flex items-start gap-2.5">
                        <div
                          className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md mt-0.5 ${
                            entitled
                              ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950/70 dark:text-emerald-300'
                              : 'bg-neutral-200 text-neutral-600 dark:bg-[#222222] dark:text-neutral-400'
                          }`}
                        >
                          {entitled ? (
                            <Check className="w-3.5 h-3.5 stroke-[2.5]" />
                          ) : (
                            <Lock className="w-3.5 h-3.5" />
                          )}
                        </div>

                        <div>
                          <h4 className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                            {item.label}
                          </h4>
                          <p className="text-[11px] text-neutral-500 dark:text-neutral-400 mt-1 leading-normal">
                            {item.description}
                          </p>
                        </div>
                      </div>
                    </div>

                    {!entitled && (
                      <div className="mt-4 pt-3 border-t border-dashed border-neutral-200 dark:border-[#262626] flex items-center justify-between">
                        <span className="text-[10px] font-medium text-amber-700 dark:text-amber-400">
                          {item.tierRequired} tier required
                        </span>
                        {hasRole('ADMIN') && (
                          <button
                            type="button"
                            onClick={() => handleUpgrade(item.tierRequired as PlanTier)}
                            className="text-[10px] font-bold text-neutral-900 dark:text-neutral-100 hover:underline inline-flex items-center gap-1 cursor-pointer"
                          >
                            Upgrade <ArrowRight className="w-3 h-3" />
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Section 4: Invoices & Payment History (Spec §12) */}
          <Card className="border-neutral-200 dark:border-[#262626]">
            <CardHeader className="flex flex-row items-center justify-between pb-3">
              <div>
                <CardTitle className="flex items-center gap-2 text-sm font-bold">
                  <CreditCard className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
                  <span>Invoice & Payment History</span>
                </CardTitle>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                  Synchronized automatically via server-side Stripe webhook events.
                </p>
              </div>
              <Badge variant="default" size="sm">
                {invoices.length} {invoices.length === 1 ? 'Invoice' : 'Invoices'}
              </Badge>
            </CardHeader>

            <CardContent className="p-0">
              {invoices.length === 0 ? (
                <EmptyState
                  icon={<FileText className="w-6 h-6" />}
                  title="No invoices generated yet"
                  description="Invoices will appear here automatically when your subscription renews or when payments are made via Stripe."
                  className="border-0 rounded-none bg-transparent"
                />
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                    <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                      <tr>
                        <th className="px-5 py-3">Invoice Number / ID</th>
                        <th className="px-5 py-3">Paid Date</th>
                        <th className="px-5 py-3">Amount</th>
                        <th className="px-5 py-3">Status</th>
                        <th className="px-5 py-3 text-right">Receipt / Document</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                      {invoices.map((inv) => (
                        <tr
                          key={inv.stripeInvoiceId}
                          className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                        >
                          <td className="px-5 py-3.5 font-mono font-medium text-neutral-900 dark:text-neutral-100">
                            {inv.stripeInvoiceId}
                          </td>
                          <td className="px-5 py-3.5">{formatDate(inv.paidAt)}</td>
                          <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                            {formatCurrency(inv.amountPaidCents, inv.currency)}
                          </td>
                          <td className="px-5 py-3.5">
                            <Badge
                              variant={inv.status === 'PAID' ? 'success' : 'warning'}
                              size="sm"
                              withDot
                            >
                              {inv.status}
                            </Badge>
                          </td>
                          <td className="px-5 py-3.5 text-right space-x-3">
                            {inv.hostedInvoiceUrl && (
                              <a
                                href={inv.hostedInvoiceUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 text-xs font-semibold text-neutral-900 hover:underline dark:text-neutral-100"
                              >
                                View <ExternalLink className="w-3 h-3" />
                              </a>
                            )}
                            {inv.invoicePdfUrl && (
                              <a
                                href={inv.invoicePdfUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 text-xs font-semibold text-neutral-500 hover:text-neutral-800 dark:hover:text-neutral-200"
                              >
                                PDF <Download className="w-3 h-3" />
                              </a>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
};
