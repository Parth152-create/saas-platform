import React, { useEffect, useState } from 'react';
import {
  Check,
  CreditCard,
  Download,
  ExternalLink,
  FileText,
  RefreshCw,
  Zap,
} from 'lucide-react';
import { billingApi } from '../../api/billingApi';
import type { BillingSummaryResponse, PlanTier } from '../../api/types';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { ErrorAlert } from '../../components/common/ErrorAlert';
import { SkeletonCard, SkeletonTable } from '../../components/common/LoadingSkeleton';
import { EmptyState } from '../../components/common/EmptyState';
import { formatCurrency, formatDate } from '../../utils/formatters';

export const BillingPage: React.FC = () => {
  const { hasRole } = useAuth();
  const { showToast } = useToast();

  const [summary, setSummary] = useState<BillingSummaryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUpgrading, setIsUpgrading] = useState<string | null>(null);
  const [isOpeningPortal, setIsOpeningPortal] = useState(false);

  const fetchBilling = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await billingApi.getBillingSummary();
      setSummary(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to retrieve billing information';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    let isMounted = true;
    billingApi
      .getBillingSummary()
      .then((data) => {
        if (isMounted) setSummary(data);
      })
      .catch((err: unknown) => {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Failed to retrieve billing information');
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, []);

  const handleUpgrade = async (planTier: PlanTier) => {
    if (!hasRole('ADMIN')) {
      showToast('error', 'Unauthorized', 'Only organization ADMINs can modify subscriptions.');
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
      const msg = err instanceof Error ? err.message : 'Stripe checkout initiation failed';
      showToast('error', 'Checkout Error', msg);
    } finally {
      setIsUpgrading(null);
    }
  };

  const handleOpenPortal = async () => {
    if (!hasRole('ADMIN')) {
      showToast('error', 'Unauthorized', 'Only organization ADMINs can access the billing portal.');
      return;
    }

    setIsOpeningPortal(true);
    try {
      const res = await billingApi.createPortalSession();
      if (res.url) {
        showToast('info', 'Opening Customer Portal', 'Redirecting to Stripe portal');
        window.location.href = res.url;
      } else {
        throw new Error('No portal session URL returned.');
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Customer portal initiation failed';
      showToast('error', 'Portal Error', msg);
    } finally {
      setIsOpeningPortal(false);
    }
  };

  const currentPlan = summary?.plan || 'FREE';
  const subscription = summary?.subscription;
  const invoices = summary?.invoices || [];

  return (
    <div className="space-y-8">
      <PageHeader
        title="Subscription & Billing"
        description="Manage your enterprise workspace subscription tier, invoices, and Stripe billing settings."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchBilling}
              isLoading={isLoading}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Sync
            </Button>
            {subscription && (
              <Button
                variant="secondary"
                size="sm"
                onClick={handleOpenPortal}
                isLoading={isOpeningPortal}
                leftIcon={<ExternalLink className="w-3.5 h-3.5" />}
              >
                Stripe Customer Portal
              </Button>
            )}
          </div>
        }
      />

      {error && (
        <ErrorAlert
          title="Billing Service Connection Error"
          message={error}
          onRetry={fetchBilling}
        />
      )}

      {isLoading ? (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <SkeletonCard />
            <SkeletonCard />
            <SkeletonCard />
          </div>
          <SkeletonTable rows={3} />
        </div>
      ) : (
        <>
          {/* Current Subscription Status Banner */}
          <Card className="p-6 bg-neutral-50 dark:bg-[#141414] border-neutral-200 dark:border-[#262626]">
            <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
              <div className="space-y-2">
                <div className="flex items-center gap-2.5">
                  <span className="text-xs font-bold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">
                    Current Active Tier
                  </span>
                  <Badge variant="primary" size="md">
                    {currentPlan} PLAN
                  </Badge>
                  {subscription && (
                    <Badge variant="success" size="sm" withDot>
                      {subscription.status}
                    </Badge>
                  )}
                </div>

                <h3 className="text-2xl font-bold text-zinc-900 dark:text-neutral-100">
                  {currentPlan === 'FREE'
                    ? 'Free Development Workspace'
                    : `${currentPlan} Enterprise Subscription`}
                </h3>

                <p className="text-xs text-zinc-600 dark:text-neutral-400 max-w-xl leading-relaxed">
                  {subscription ? (
                    <>
                      Current billing period ends{' '}
                      <strong>{formatDate(subscription.currentPeriodEnd)}</strong>.
                      {subscription.cancelAtPeriodEnd && (
                        <span className="text-amber-600 dark:text-amber-400 font-semibold ml-1">
                          (Set to cancel at end of period)
                        </span>
                      )}
                    </>
                  ) : (
                    'Your tenant is on the default tier. Upgrade to PRO or ENTERPRISE to unlock higher employee quotas, priority support, and multi-region database schemas.'
                  )}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-3">
                {subscription ? (
                  <Button
                    onClick={handleOpenPortal}
                    isLoading={isOpeningPortal}
                    leftIcon={<ExternalLink className="w-4 h-4" />}
                  >
                    Manage in Customer Portal
                  </Button>
                ) : (
                  <Button
                    onClick={() => handleUpgrade('PRO')}
                    isLoading={isUpgrading === 'PRO'}
                    leftIcon={<Zap className="w-4 h-4" />}
                  >
                    Upgrade to PRO Plan
                  </Button>
                )}
              </div>
            </div>
          </Card>

          {/* Pricing Tiers Comparison */}
          <div className="space-y-4">
            <div>
              <h3 className="text-base font-bold text-zinc-900 dark:text-neutral-100">
                Available Workspace Plans
              </h3>
              <p className="text-xs text-zinc-500 dark:text-neutral-400">
                Direct Stripe Checkout session integration with server-side webhook synchronization
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {/* FREE Plan */}
              <Card
                className={`p-6 flex flex-col justify-between ${
                  currentPlan === 'FREE'
                    ? 'border-zinc-950 ring-2 ring-zinc-950/10 dark:border-white dark:ring-white/10'
                    : ''
                }`}
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-base font-bold text-zinc-900 dark:text-neutral-100">
                      Starter Free
                    </h4>
                    {currentPlan === 'FREE' && (
                      <Badge variant="primary" size="sm">
                        Current
                      </Badge>
                    )}
                  </div>
                  <div>
                    <span className="text-3xl font-bold text-zinc-900 dark:text-neutral-100">$0</span>
                    <span className="text-xs text-zinc-400"> / month</span>
                  </div>
                  <p className="text-xs text-neutral-500">
                    Essential features for evaluating the SaaS multi-tenant architecture.
                  </p>
                  <ul className="space-y-2 text-xs text-neutral-600 dark:text-neutral-400 border-t border-neutral-100 dark:border-[#262626] pt-4">
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Up to 10 Employees</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Basic Attendance Tracking</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Standard PostgreSQL Schema</span>
                    </li>
                  </ul>
                </div>
                <div className="pt-6">
                  <Button variant="secondary" size="sm" className="w-full" disabled>
                    {currentPlan === 'FREE' ? 'Active Plan' : 'Free Included'}
                  </Button>
                </div>
              </Card>

              {/* PRO Plan */}
              <Card
                className={`p-6 flex flex-col justify-between relative ${
                  currentPlan === 'PRO'
                    ? 'border-neutral-950 ring-2 ring-neutral-950/10 dark:border-white dark:ring-white/10'
                    : 'border-neutral-200 dark:border-[#262626]'
                }`}
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                      Professional
                    </h4>
                    {currentPlan === 'PRO' ? (
                      <Badge variant="primary" size="sm">
                        Current
                      </Badge>
                    ) : (
                      <Badge variant="default" size="sm">
                        Popular
                      </Badge>
                    )}
                  </div>
                  <div>
                    <span className="text-3xl font-bold text-neutral-900 dark:text-neutral-100">$49</span>
                    <span className="text-xs text-neutral-400"> / month</span>
                  </div>
                  <p className="text-xs text-neutral-500">
                    Advanced workforce management for growing enterprise teams.
                  </p>
                  <ul className="space-y-2 text-xs text-neutral-600 dark:text-neutral-400 border-t border-neutral-100 dark:border-[#262626] pt-4">
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Up to 100 Employees</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Automated Stripe Invoicing</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Customer Self-Service Portal</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Audit Log Retention</span>
                    </li>
                  </ul>
                </div>
                <div className="pt-6">
                  {currentPlan === 'PRO' ? (
                    <Button variant="outline" size="sm" className="w-full" onClick={handleOpenPortal}>
                      Manage Subscription
                    </Button>
                  ) : (
                    <Button
                      size="sm"
                      className="w-full"
                      onClick={() => handleUpgrade('PRO')}
                      isLoading={isUpgrading === 'PRO'}
                    >
                      Upgrade to PRO
                    </Button>
                  )}
                </div>
              </Card>

              {/* ENTERPRISE Plan */}
              <Card
                className={`p-6 flex flex-col justify-between ${
                  currentPlan === 'ENTERPRISE'
                    ? 'border-neutral-950 ring-2 ring-neutral-950/10 dark:border-white dark:ring-white/10'
                    : 'border-neutral-200 dark:border-[#262626]'
                }`}
              >
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <h4 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                      Enterprise Suite
                    </h4>
                    {currentPlan === 'ENTERPRISE' && (
                      <Badge variant="primary" size="sm">
                        Current
                      </Badge>
                    )}
                  </div>
                  <div>
                    <span className="text-3xl font-bold text-neutral-900 dark:text-neutral-100">$199</span>
                    <span className="text-xs text-neutral-400"> / month</span>
                  </div>
                  <p className="text-xs text-neutral-500">
                    Unlimited scale, dedicated support, and custom integrations.
                  </p>
                  <ul className="space-y-2 text-xs text-neutral-600 dark:text-neutral-400 border-t border-neutral-100 dark:border-[#262626] pt-4">
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Unlimited Employees & Teams</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Custom Work Schedule Models</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Priority SLA & Dedicated CSM</span>
                    </li>
                    <li className="flex items-center gap-2">
                      <Check className="w-4 h-4 text-emerald-500 shrink-0" />
                      <span>Custom Domain & SSO Enforce</span>
                    </li>
                  </ul>
                </div>
                <div className="pt-6">
                  {currentPlan === 'ENTERPRISE' ? (
                    <Button variant="outline" size="sm" className="w-full" onClick={handleOpenPortal}>
                      Manage Subscription
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      className="w-full"
                      onClick={() => handleUpgrade('ENTERPRISE')}
                      isLoading={isUpgrading === 'ENTERPRISE'}
                    >
                      Upgrade to Enterprise
                    </Button>
                  )}
                </div>
              </Card>
            </div>
          </div>

          {/* Invoices History Table */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
                  <span>Invoice & Payment History</span>
                </CardTitle>
                <p className="text-xs text-zinc-500 dark:text-neutral-400 mt-1">
                  Synchronized automatically via Stripe webhook events
                </p>
              </div>
              <Badge variant="default" size="sm">
                {invoices.length} Invoices
              </Badge>
            </CardHeader>
            <CardContent className="p-0">
              {invoices.length === 0 ? (
                <EmptyState
                  icon={<FileText className="w-6 h-6" />}
                  title="No invoices generated yet"
                  description="Invoices will appear here automatically when your subscription renews or when paid via Stripe."
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
                        <th className="px-5 py-3 text-right">Receipt / PDF</th>
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
                          <td className="px-5 py-3.5 text-right space-x-2">
                            {inv.hostedInvoiceUrl && (
                              <a
                                href={inv.hostedInvoiceUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 text-xs font-semibold text-zinc-900 hover:underline dark:text-neutral-100"
                              >
                                View <ExternalLink className="w-3 h-3" />
                              </a>
                            )}
                            {inv.invoicePdfUrl && (
                              <a
                                href={inv.invoicePdfUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="inline-flex items-center gap-1 text-xs font-semibold text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
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
