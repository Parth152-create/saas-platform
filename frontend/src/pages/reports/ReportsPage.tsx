import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BarChart3,
  Download,
  FileSpreadsheet,
  GitBranch,
  Lock,
  RefreshCw,
  Sparkles,
  TrendingUp,
  Zap,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { SimpleBarChart, SimpleDonutChart } from '../../components/widgets/SimpleChart';
import { useToast } from '../../context/ToastContext';
import { useAuth } from '../../context/AuthContext';
import { useEntitlements } from '../../context/EntitlementsContext';
import {
  reportsApi,
  type AnalyticsResponse,
  type BasicReportsResponse,
  type AdvancedReportsResponse,
  type CustomWorkflowsResponse,
} from '../../api/reportsApi';

export const ReportsPage: React.FC = () => {
  const { hasRole } = useAuth();
  const { hasFeature, plan, isLoading: entitlementsLoading } = useEntitlements();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const canAccessAdvancedReports = hasFeature('ADVANCED_REPORTS');
  const canAccessAnalytics = hasFeature('ADVANCED_ANALYTICS');
  const canAccessWorkflows = hasFeature('CUSTOM_WORKFLOWS');

  const [basicData, setBasicData] = useState<BasicReportsResponse | null>(null);
  const [advancedData, setAdvancedData] = useState<AdvancedReportsResponse | null>(null);
  const [analyticsData, setAnalyticsData] = useState<AnalyticsResponse | null>(null);
  const [workflowsData, setWorkflowsData] = useState<CustomWorkflowsResponse | null>(null);
  const [isExporting, setIsExporting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const promises: Promise<void>[] = [];

      if (hasFeature('BASIC_REPORTS')) {
        promises.push(
          reportsApi.getBasicReports().then(setBasicData).catch(() => setBasicData(null))
        );
      }

      if (canAccessAdvancedReports) {
        promises.push(
          reportsApi.getAdvancedReports().then(setAdvancedData).catch(() => setAdvancedData(null))
        );
      } else {
        setAdvancedData(null);
      }

      if (canAccessAnalytics) {
        promises.push(
          reportsApi.getAnalytics().then(setAnalyticsData).catch(() => setAnalyticsData(null))
        );
      } else {
        setAnalyticsData(null);
      }

      if (canAccessWorkflows) {
        promises.push(
          reportsApi.getCustomWorkflows().then(setWorkflowsData).catch(() => setWorkflowsData(null))
        );
      } else {
        setWorkflowsData(null);
      }

      await Promise.all(promises);
    } finally {
      setIsLoading(false);
    }
  }, [canAccessAdvancedReports, canAccessAnalytics, canAccessWorkflows, hasFeature]);

  useEffect(() => {
    if (entitlementsLoading) return;
    let isMounted = true;
    const promises: Promise<void>[] = [];

    if (hasFeature('BASIC_REPORTS')) {
      promises.push(
        reportsApi.getBasicReports().then((data) => {
          if (isMounted) setBasicData(data);
        }).catch(() => {})
      );
    }

    if (canAccessAdvancedReports) {
      promises.push(
        reportsApi.getAdvancedReports().then((data) => {
          if (isMounted) setAdvancedData(data);
        }).catch(() => {})
      );
    }

    if (canAccessAnalytics) {
      promises.push(
        reportsApi.getAnalytics().then((data) => {
          if (isMounted) setAnalyticsData(data);
        }).catch(() => {})
      );
    }

    if (canAccessWorkflows) {
      promises.push(
        reportsApi.getCustomWorkflows().then((data) => {
          if (isMounted) setWorkflowsData(data);
        }).catch(() => {})
      );
    }

    Promise.all(promises).finally(() => {
      if (isMounted) setIsLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, [entitlementsLoading, canAccessAdvancedReports, canAccessAnalytics, canAccessWorkflows, hasFeature]);

  const handleExport = async () => {
    if (!canAccessAdvancedReports) {
      showToast(
        'warning',
        'Pro Subscription Required',
        'Comprehensive workforce reporting requires a Pro or Enterprise subscription.'
      );
      return;
    }

    if (!hasRole('ADMIN')) {
      showToast(
        'error',
        'Admin Role Required',
        'Only administrators can trigger comprehensive compliance exports.'
      );
      return;
    }

    setIsExporting(true);
    try {
      const res = await reportsApi.exportAdvancedReportAsAdmin();
      showToast(
        'success',
        'Export Generated',
        `Admin export completed (${res.exportStatus}) for ${res.feature}.`
      );
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Export failed';
      showToast('error', 'Export Error', msg);
    } finally {
      setIsExporting(false);
    }
  };

  // Base list of reports with tier gating dynamically augmented from backend
  const reportItems = [
    {
      name: basicData?.data?.[0] ? `${basicData.data[0]} & Roster.pdf` : 'Headcount Summary & Roster.pdf',
      date: 'Sep 15, 2026',
      size: '1.8 MB',
      type: 'BASIC' as const,
      requiredTier: 'STARTER' as const,
      isEntitled: hasFeature('BASIC_REPORTS'),
    },
    {
      name: basicData?.data?.[1] ? `Organization ${basicData.data[1]}.xlsx` : 'Organization Attendance & Timesheet Log.xlsx',
      date: 'Sep 01, 2026',
      size: '1.2 MB',
      type: 'BASIC' as const,
      requiredTier: 'STARTER' as const,
      isEntitled: hasFeature('BASIC_REPORTS'),
    },
    {
      name: advancedData?.data?.[0] ? `${advancedData.data[0]} Q3-2026.pdf` : 'Monthly Workforce Cost & Billable Utilization Q3-2026.pdf',
      date: 'Sep 15, 2026',
      size: '3.4 MB',
      type: 'ADVANCED' as const,
      requiredTier: 'PRO' as const,
      isEntitled: canAccessAdvancedReports,
    },
    {
      name: advancedData?.data?.[1] ? `${advancedData.data[1]} Forecast.xlsx` : 'PTO Accrual & Absence Projection Forecast.xlsx',
      date: 'Sep 10, 2026',
      size: '2.1 MB',
      type: 'ADVANCED' as const,
      requiredTier: 'PRO' as const,
      isEntitled: canAccessAdvancedReports,
    },
  ];

  const runRateFormatted = analyticsData?.metrics?.runRate
    ? `$${Math.round(analyticsData.metrics.runRate / 1000)}k`
    : '$248k';

  return (
    <div className="space-y-6">
      <PageHeader
        title="Analytics & HR Reports"
        description="Consolidated workforce analytics, payroll forecasting, and compliance audit exports."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={loadData}
              isLoading={isLoading}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Sync
            </Button>
            <Button
              size="sm"
              onClick={handleExport}
              isLoading={isExporting}
              leftIcon={
                canAccessAdvancedReports ? (
                  <Download className="w-4 h-4" />
                ) : (
                  <Lock className="w-4 h-4 text-amber-300" />
                )
              }
            >
              {canAccessAdvancedReports
                ? 'Export Comprehensive Report'
                : 'Unlock Comprehensive Report'}
            </Button>
          </div>
        }
      />

      {/* Plan Status Notice when on Starter */}
      {!canAccessAnalytics && !entitlementsLoading && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between p-4 rounded-xl border border-amber-200 dark:border-amber-900/40 bg-amber-50/70 dark:bg-amber-950/20 gap-3">
          <div className="flex items-center gap-3">
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-amber-100 dark:bg-amber-900/60 text-amber-800 dark:text-amber-200">
              <Lock className="w-5 h-5" />
            </div>
            <div>
              <h4 className="text-xs font-bold text-amber-950 dark:text-amber-200">
                You are on the {plan} Plan
              </h4>
              <p className="text-[11px] text-amber-800 dark:text-amber-300/80">
                Advanced workforce analytics, headcount projections, and custom approval workflows require a Pro or Enterprise subscription.
              </p>
            </div>
          </div>
          <Button
            size="sm"
            variant="primary"
            onClick={() => navigate('/app/settings/subscription')}
            leftIcon={<Zap className="w-3.5 h-3.5" />}
            className="shrink-0"
          >
            Upgrade Plan
          </Button>
        </div>
      )}

      {/* Section 1: Analytics & Visual Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Headcount Chart */}
        <Card className="relative overflow-hidden">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <TrendingUp className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
              <span>Headcount Growth vs Plan</span>
            </CardTitle>
            {!canAccessAnalytics && (
              <Badge variant="warning" size="sm" withDot>
                PRO REQUIRED
              </Badge>
            )}
          </CardHeader>
          <CardContent className="pt-2">
            {canAccessAnalytics ? (
              <SimpleBarChart
                data={[
                  { label: 'Q1', value: 32 },
                  { label: 'Q2', value: 39 },
                  { label: 'Q3', value: 44 },
                  { label: 'Q4 (Target)', value: 50 },
                ]}
                height={180}
                valueFormatter={(v) => `${v} staff`}
              />
            ) : (
              <div className="flex flex-col items-center justify-center p-8 text-center space-y-3 bg-neutral-50/70 dark:bg-[#111111]/70 rounded-xl border border-dashed border-neutral-200 dark:border-[#262626]">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-neutral-200 dark:bg-[#222222] text-neutral-600 dark:text-neutral-300">
                  <Lock className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                    Headcount Growth Analytics Locked
                  </h4>
                  <p className="text-[11px] text-neutral-500 dark:text-neutral-400 mt-1 max-w-sm">
                    Interactive quarterly headcount forecasting requires the Pro or Enterprise subscription tier.
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate('/app/settings/subscription')}
                >
                  View Subscription Plans
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Department Cost Allocation */}
        <Card className="relative overflow-hidden">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <BarChart3 className="w-4 h-4 text-zinc-900 dark:text-neutral-100" />
              <span>Department Cost Allocation</span>
            </CardTitle>
            {!canAccessAnalytics && (
              <Badge variant="warning" size="sm" withDot>
                PRO REQUIRED
              </Badge>
            )}
          </CardHeader>
          <CardContent className="pt-2">
            {canAccessAnalytics ? (
              <SimpleDonutChart
                segments={[
                  { label: 'Engineering', value: 45, color: '#18181b', darkColor: '#f4f4f5' },
                  { label: 'Product & Design', value: 20, color: '#52525b', darkColor: '#d4d4d8' },
                  { label: 'Operations', value: 15, color: '#71717a', darkColor: '#a1a1aa' },
                  { label: 'Sales & Marketing', value: 20, color: '#a1a1aa', darkColor: '#71717a' },
                ]}
                centerText={runRateFormatted}
                centerSubtext="Monthly Run Rate"
                size={170}
              />
            ) : (
              <div className="flex flex-col items-center justify-center p-8 text-center space-y-3 bg-neutral-50/70 dark:bg-[#111111]/70 rounded-xl border border-dashed border-neutral-200 dark:border-[#262626]">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-neutral-200 dark:bg-[#222222] text-neutral-600 dark:text-neutral-300">
                  <Lock className="w-5 h-5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                    Workforce Cost Allocation Locked
                  </h4>
                  <p className="text-[11px] text-neutral-500 dark:text-neutral-400 mt-1 max-w-sm">
                    Detailed department expense breakdowns and monthly run rate analysis are part of the Pro tier.
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => navigate('/app/settings/subscription')}
                >
                  View Subscription Plans
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Section 2: Enterprise Custom Workflows (Spec §3 & §11) */}
      {canAccessWorkflows ? (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-sm font-bold">
              <GitBranch className="w-4 h-4 text-purple-600 dark:text-purple-400" />
              <span>Enterprise Custom Workflows</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {workflowsData?.workflows ? (
                workflowsData.workflows.map((wf) => (
                  <div
                    key={wf}
                    className="p-3.5 rounded-lg border border-neutral-200 dark:border-[#262626] bg-neutral-50/50 dark:bg-[#141414] flex items-center justify-between"
                  >
                    <span className="text-xs font-semibold text-neutral-900 dark:text-neutral-100">{wf}</span>
                    <Badge variant="success" size="sm">Active</Badge>
                  </div>
                ))
              ) : (
                <p className="text-xs text-neutral-500">Custom multi-level approval matrices configured.</p>
              )}
            </div>
          </CardContent>
        </Card>
      ) : (
        <Card className="p-4 border-dashed border-neutral-200 dark:border-[#262626] bg-neutral-50/40 dark:bg-[#111111]/40 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-neutral-100 dark:bg-[#1f1f1f] text-neutral-600 dark:text-neutral-400">
              <GitBranch className="w-4 h-4" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h4 className="text-xs font-bold text-neutral-900 dark:text-neutral-100">
                  Custom Approval Workflows & Multi-Step Escalations
                </h4>
                <Badge variant="default" size="sm">ENTERPRISE</Badge>
              </div>
              <p className="text-[11px] text-neutral-500 dark:text-neutral-400 mt-0.5">
                Build custom organizational approval hierarchies, department matrices, and automated escalation paths.
              </p>
            </div>
          </div>
          <Button
            size="sm"
            variant="outline"
            onClick={() => navigate('/app/settings/subscription')}
            leftIcon={<Sparkles className="w-3.5 h-3.5 text-purple-600 dark:text-purple-400" />}
            className="shrink-0"
          >
            Upgrade to Enterprise
          </Button>
        </Card>
      )}

      {/* Section 3: Generated Reports Catalog */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-3">
          <div>
            <CardTitle className="text-sm font-bold">Operational Reports Catalog</CardTitle>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
              Exports dynamically gated by your organization’s active subscription tier.
            </p>
          </div>
          <Badge variant="default" size="sm">
            {reportItems.filter((r) => r.isEntitled).length} of {reportItems.length} Available
          </Badge>
        </CardHeader>
        <CardContent className="p-0">
          <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
            {reportItems.map((item, idx) => (
              <div
                key={idx}
                className="flex items-center justify-between p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg ${
                      item.isEntitled
                        ? 'bg-neutral-100 text-neutral-800 dark:bg-[#1f1f1f] dark:text-neutral-200'
                        : 'bg-neutral-100 text-neutral-400 dark:bg-[#141414] dark:text-neutral-600'
                    }`}
                  >
                    {item.isEntitled ? (
                      <FileSpreadsheet className="w-4 h-4" />
                    ) : (
                      <Lock className="w-4 h-4" />
                    )}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <p
                        className={`text-xs font-semibold ${
                          item.isEntitled
                            ? 'text-neutral-900 dark:text-neutral-100'
                            : 'text-neutral-500 dark:text-neutral-400'
                        }`}
                      >
                        {item.name}
                      </p>
                      {!item.isEntitled && (
                        <Badge variant="warning" size="sm">
                          {item.requiredTier} Plan
                        </Badge>
                      )}
                    </div>
                    <p className="text-[11px] text-neutral-400">
                      Generated {item.date} • {item.size}
                    </p>
                  </div>
                </div>

                {item.isEntitled ? (
                  <Button
                    variant="outline"
                    size="sm"
                    leftIcon={<Download className="w-3.5 h-3.5" />}
                    onClick={() => showToast('success', 'Download Complete', item.name)}
                  >
                    Download
                  </Button>
                ) : (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => navigate('/app/settings/subscription')}
                    leftIcon={<Lock className="w-3.5 h-3.5" />}
                    className="text-amber-700 dark:text-amber-400 hover:text-amber-800"
                  >
                    Upgrade to Access
                  </Button>
                )}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
