import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BarChart3,
  Calendar,
  CheckSquare,
  Download,
  FileSpreadsheet,
  Filter,
  FolderKanban,
  GitBranch,
  Lock,
  RefreshCw,
  Sparkles,
  TrendingUp,
  Users,
  Zap,
} from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Tabs } from '../../components/common/Tabs';
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
import type {
  LeaveReport,
  ProjectsReport,
  TasksReport,
  WorkforceReport,
} from '../../api/types';
import { formatDate } from '../../utils/formatters';

type ReportTab = 'overview' | 'workforce' | 'projects' | 'tasks' | 'leave';

export const ReportsPage: React.FC = () => {
  const { hasRole } = useAuth();
  const { hasFeature, plan, isLoading: entitlementsLoading } = useEntitlements();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const isManagerOrAdmin = hasRole('MANAGER') || hasRole('ADMIN') || hasRole('SUPER_ADMIN');

  const canAccessAdvancedReports = hasFeature('ADVANCED_REPORTS');
  const canAccessAnalytics = hasFeature('ADVANCED_ANALYTICS');
  const canAccessWorkflows = hasFeature('CUSTOM_WORKFLOWS');

  const [activeTab, setActiveTab] = useState<ReportTab>('overview');

  // Overview data
  const [basicData, setBasicData] = useState<BasicReportsResponse | null>(null);
  const [advancedData, setAdvancedData] = useState<AdvancedReportsResponse | null>(null);
  const [analyticsData, setAnalyticsData] = useState<AnalyticsResponse | null>(null);
  const [workflowsData, setWorkflowsData] = useState<CustomWorkflowsResponse | null>(null);

  // Real v1.1 Persisted Data Reports
  const [workforceReport, setWorkforceReport] = useState<WorkforceReport | null>(null);
  const [projectsReport, setProjectsReport] = useState<ProjectsReport | null>(null);
  const [tasksReport, setTasksReport] = useState<TasksReport | null>(null);
  const [leaveReport, setLeaveReport] = useState<LeaveReport | null>(null);

  // Filters
  const [wfDepartmentFilter, setWfDepartmentFilter] = useState('ALL');
  const [wfStatusFilter, setWfStatusFilter] = useState('ALL');
  const [projStatusFilter, setProjStatusFilter] = useState('ALL');
  const [taskStatusFilter, setTaskStatusFilter] = useState('ALL');
  const [leaveStatusFilter, setLeaveStatusFilter] = useState('ALL');

  const [isLoading, setIsLoading] = useState(false);
  const [isExporting, setIsExporting] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const promises: Promise<unknown>[] = [];

      if (hasFeature('BASIC_REPORTS')) {
        promises.push(
          reportsApi.getBasicReports().then(setBasicData).catch(() => setBasicData(null))
        );
      }

      if (canAccessAdvancedReports) {
        promises.push(
          reportsApi.getAdvancedReports().then(setAdvancedData).catch(() => setAdvancedData(null))
        );
      }

      if (canAccessAnalytics) {
        promises.push(
          reportsApi.getAnalytics().then(setAnalyticsData).catch(() => setAnalyticsData(null))
        );
      }

      if (canAccessWorkflows) {
        promises.push(
          reportsApi.getCustomWorkflows().then(setWorkflowsData).catch(() => setWorkflowsData(null))
        );
      }

      if (isManagerOrAdmin) {
        promises.push(
          reportsApi.getWorkforceReport(wfDepartmentFilter, wfStatusFilter).then(setWorkforceReport).catch(() => null),
          reportsApi.getProjectsReport(projStatusFilter).then(setProjectsReport).catch(() => null),
          reportsApi.getTasksReport(undefined, taskStatusFilter).then(setTasksReport).catch(() => null),
          reportsApi.getLeaveReport(leaveStatusFilter).then(setLeaveReport).catch(() => null)
        );
      }

      await Promise.all(promises);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error loading reports';
      showToast('error', 'Sync Failed', msg);
    } finally {
      setIsLoading(false);
    }
  }, [
    hasFeature,
    canAccessAdvancedReports,
    canAccessAnalytics,
    canAccessWorkflows,
    isManagerOrAdmin,
    wfDepartmentFilter,
    wfStatusFilter,
    projStatusFilter,
    taskStatusFilter,
    leaveStatusFilter,
    showToast,
  ]);

  useEffect(() => {
    if (entitlementsLoading) return;
    let isMounted = true;
    const promises: Promise<unknown>[] = [];

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

    if (isManagerOrAdmin) {
      promises.push(
        reportsApi.getWorkforceReport(wfDepartmentFilter, wfStatusFilter).then((data) => {
          if (isMounted) setWorkforceReport(data);
        }).catch(() => {}),
        reportsApi.getProjectsReport(projStatusFilter).then((data) => {
          if (isMounted) setProjectsReport(data);
        }).catch(() => {}),
        reportsApi.getTasksReport(undefined, taskStatusFilter).then((data) => {
          if (isMounted) setTasksReport(data);
        }).catch(() => {}),
        reportsApi.getLeaveReport(leaveStatusFilter).then((data) => {
          if (isMounted) setLeaveReport(data);
        }).catch(() => {})
      );
    }

    Promise.all(promises).finally(() => {
      if (isMounted) setIsLoading(false);
    });

    return () => {
      isMounted = false;
    };
  }, [
    entitlementsLoading,
    hasFeature,
    canAccessAdvancedReports,
    canAccessAnalytics,
    canAccessWorkflows,
    isManagerOrAdmin,
    wfDepartmentFilter,
    wfStatusFilter,
    projStatusFilter,
    taskStatusFilter,
    leaveStatusFilter,
  ]);

  const handleDownloadCsv = async (type: 'workforce' | 'projects' | 'tasks' | 'leave') => {
    if (!isManagerOrAdmin) {
      showToast('error', 'Unauthorized', 'Manager or Administrator role required for compliance CSV exports.');
      return;
    }

    try {
      setIsExporting(type);
      await reportsApi.downloadCsvExport(type);
      showToast('success', 'Export Downloaded', `${type.toUpperCase()} CSV export file generated successfully.`);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Export failed';
      showToast('error', 'Export Error', msg);
    } finally {
      setIsExporting(null);
    }
  };

  const tabs = [
    { id: 'overview', label: 'Overview & Forecasts', icon: <TrendingUp className="w-4 h-4" /> },
    ...(isManagerOrAdmin
      ? [
          { id: 'workforce', label: 'Workforce Report', icon: <Users className="w-4 h-4" /> },
          { id: 'projects', label: 'Projects Report', icon: <FolderKanban className="w-4 h-4" /> },
          { id: 'tasks', label: 'Tasks Report', icon: <CheckSquare className="w-4 h-4" /> },
          { id: 'leave', label: 'Leave Report', icon: <Calendar className="w-4 h-4" /> },
        ]
      : []),
  ];

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
        title="Analytics &amp; Operational Reports"
        description="Real-time workforce metrics, project delivery tracking, leave liability, and CSV compliance export."
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
          </div>
        }
      />

      {/* Tabs */}
      <div className="border-b border-neutral-200 dark:border-[#262626]">
        <Tabs
          tabs={tabs}
          activeTab={activeTab}
          onChange={(tab) => setActiveTab(tab as ReportTab)}
        />
      </div>

      {/* Tab 1: Overview & Forecasts */}
      {activeTab === 'overview' && (
        <div className="space-y-6">
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

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
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

          {/* Enterprise Workflows */}
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
                      Custom Approval Workflows &amp; Multi-Step Escalations
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
      )}

      {/* Tab 2: Workforce Report */}
      {activeTab === 'workforce' && isManagerOrAdmin && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-neutral-400" />
              <select
                value={wfDepartmentFilter}
                onChange={(e) => setWfDepartmentFilter(e.target.value)}
                className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-3 py-1.5"
              >
                <option value="ALL">All Departments</option>
                {workforceReport &&
                  Object.keys(workforceReport.departmentDistribution || {}).map((dep) => (
                    <option key={dep} value={dep}>{dep}</option>
                  ))}
              </select>
              <select
                value={wfStatusFilter}
                onChange={(e) => setWfStatusFilter(e.target.value)}
                className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-3 py-1.5"
              >
                <option value="ALL">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="ON_LEAVE">On Leave</option>
                <option value="PROBATION">Probation</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={() => handleDownloadCsv('workforce')}
              isLoading={isExporting === 'workforce'}
              leftIcon={<Download className="w-3.5 h-3.5" />}
            >
              Export Workforce CSV
            </Button>
          </div>

          {/* KPIs */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <Card className="p-4">
              <span className="text-xs text-neutral-400 font-medium uppercase">Total Headcount</span>
              <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                {workforceReport?.totalEmployees ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-emerald-500 font-medium uppercase">Active Staff</span>
              <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {workforceReport?.activeEmployees ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-amber-500 font-medium uppercase">On Leave</span>
              <p className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                {workforceReport?.onLeaveEmployees ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-neutral-400 font-medium uppercase">Probation</span>
              <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                {workforceReport?.probationEmployees ?? 0}
              </p>
            </Card>
          </div>

          {/* Employee Roster Table */}
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Code / Name</th>
                      <th className="px-5 py-3">Department</th>
                      <th className="px-5 py-3">Position</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Hire Date</th>
                      <th className="px-5 py-3 text-right">Billable Hours</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {(workforceReport?.employees || []).map((emp) => (
                      <tr key={emp.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                        <td className="px-5 py-3.5">
                          <div className="font-semibold text-neutral-900 dark:text-neutral-100">{emp.name}</div>
                          <div className="text-[11px] text-neutral-400">{emp.employeeId} • {emp.email}</div>
                        </td>
                        <td className="px-5 py-3.5">{emp.department}</td>
                        <td className="px-5 py-3.5">{emp.position}</td>
                        <td className="px-5 py-3.5">
                          <Badge variant={emp.status === 'ACTIVE' ? 'success' : 'default'} size="sm">
                            {emp.status}
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5 whitespace-nowrap">{emp.hireDate ? formatDate(emp.hireDate) : '—'}</td>
                        <td className="px-5 py-3.5 text-right font-mono font-semibold text-neutral-900 dark:text-neutral-100">
                          {emp.billableHours}h
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 3: Projects Report */}
      {activeTab === 'projects' && isManagerOrAdmin && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-neutral-400" />
              <select
                value={projStatusFilter}
                onChange={(e) => setProjStatusFilter(e.target.value)}
                className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-3 py-1.5"
              >
                <option value="ALL">All Project Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="PLANNING">Planning</option>
                <option value="COMPLETED">Completed</option>
                <option value="ON_HOLD">On Hold</option>
              </select>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={() => handleDownloadCsv('projects')}
              isLoading={isExporting === 'projects'}
              leftIcon={<Download className="w-3.5 h-3.5" />}
            >
              Export Projects CSV
            </Button>
          </div>

          {/* KPIs */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <Card className="p-4">
              <span className="text-xs text-neutral-400 font-medium uppercase">Total Projects</span>
              <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                {projectsReport?.totalProjects ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-blue-500 font-medium uppercase">Active Delivery</span>
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
                {projectsReport?.activeProjects ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-emerald-500 font-medium uppercase">Completed</span>
              <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {projectsReport?.completedProjects ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-amber-500 font-medium uppercase">On Hold</span>
              <p className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                {projectsReport?.onHoldProjects ?? 0}
              </p>
            </Card>
          </div>

          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Project</th>
                      <th className="px-5 py-3">Client</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Priority</th>
                      <th className="px-5 py-3">Tasks (Done / Total)</th>
                      <th className="px-5 py-3">Progress</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {(projectsReport?.projects || []).map((p) => (
                      <tr key={p.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                        <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">{p.name}</td>
                        <td className="px-5 py-3.5 text-neutral-500">{p.client || 'Internal'}</td>
                        <td className="px-5 py-3.5">
                          <Badge variant="default" size="sm">{p.status}</Badge>
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant={p.priority === 'HIGH' ? 'danger' : 'default'} size="sm">{p.priority}</Badge>
                        </td>
                        <td className="px-5 py-3.5 font-mono">{p.doneTasks} / {p.totalTasks}</td>
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-[11px]">{p.progressPercentage}%</span>
                            <div className="w-24 bg-neutral-200 dark:bg-[#262626] rounded-full h-1.5 overflow-hidden">
                              <div
                                className="h-1.5 rounded-full bg-neutral-900 dark:bg-white"
                                style={{ width: `${p.progressPercentage}%` }}
                              />
                            </div>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 4: Tasks Report */}
      {activeTab === 'tasks' && isManagerOrAdmin && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-neutral-400" />
              <select
                value={taskStatusFilter}
                onChange={(e) => setTaskStatusFilter(e.target.value)}
                className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-3 py-1.5"
              >
                <option value="ALL">All Task Statuses</option>
                <option value="BACKLOG">Backlog</option>
                <option value="TODO">To Do</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="IN_REVIEW">In Review</option>
                <option value="DONE">Done</option>
              </select>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={() => handleDownloadCsv('tasks')}
              isLoading={isExporting === 'tasks'}
              leftIcon={<Download className="w-3.5 h-3.5" />}
            >
              Export Tasks CSV
            </Button>
          </div>

          {/* KPIs */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
            <Card className="p-4">
              <span className="text-xs text-neutral-400 font-medium uppercase">Total Tasks</span>
              <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                {tasksReport?.totalTasks ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-blue-500 font-medium uppercase">Open Tasks</span>
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
                {tasksReport?.openTasks ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-emerald-500 font-medium uppercase">Completed</span>
              <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {tasksReport?.completedTasks ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-red-500 font-medium uppercase">Overdue</span>
              <p className="text-2xl font-bold text-red-600 dark:text-red-400 mt-1">
                {tasksReport?.overdueTasks ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-neutral-400 font-medium uppercase">Completion Rate</span>
              <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                {Math.round(tasksReport?.completionRate ?? 0)}%
              </p>
            </Card>
          </div>

          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Task Title</th>
                      <th className="px-5 py-3">Project</th>
                      <th className="px-5 py-3">Assignee</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Priority</th>
                      <th className="px-5 py-3">Due Date</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {(tasksReport?.tasks || []).map((t) => (
                      <tr key={t.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                        <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100 max-w-xs truncate">{t.title}</td>
                        <td className="px-5 py-3.5 text-neutral-500">{t.projectName}</td>
                        <td className="px-5 py-3.5">{t.assigneeName || 'Unassigned'}</td>
                        <td className="px-5 py-3.5">
                          <Badge variant={t.status === 'DONE' ? 'success' : 'default'} size="sm">{t.status}</Badge>
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant={t.priority === 'URGENT' ? 'danger' : 'default'} size="sm">{t.priority}</Badge>
                        </td>
                        <td className="px-5 py-3.5 whitespace-nowrap">{t.dueDate ? formatDate(t.dueDate) : '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tab 5: Leave Report */}
      {activeTab === 'leave' && isManagerOrAdmin && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <Filter className="w-4 h-4 text-neutral-400" />
              <select
                value={leaveStatusFilter}
                onChange={(e) => setLeaveStatusFilter(e.target.value)}
                className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-3 py-1.5"
              >
                <option value="ALL">All Statuses</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <Button
              variant="primary"
              size="sm"
              onClick={() => handleDownloadCsv('leave')}
              isLoading={isExporting === 'leave'}
              leftIcon={<Download className="w-3.5 h-3.5" />}
            >
              Export Leave CSV
            </Button>
          </div>

          {/* KPIs */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-4">
            <Card className="p-4">
              <span className="text-xs text-neutral-400 font-medium uppercase">Total Requests</span>
              <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">
                {leaveReport?.totalRequests ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-amber-500 font-medium uppercase">Pending Review</span>
              <p className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">
                {leaveReport?.pendingRequests ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-emerald-500 font-medium uppercase">Approved</span>
              <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 mt-1">
                {leaveReport?.approvedRequests ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-red-500 font-medium uppercase">Rejected</span>
              <p className="text-2xl font-bold text-red-600 dark:text-red-400 mt-1">
                {leaveReport?.rejectedRequests ?? 0}
              </p>
            </Card>
            <Card className="p-4">
              <span className="text-xs text-blue-500 font-medium uppercase">Days Approved</span>
              <p className="text-2xl font-bold text-blue-600 dark:text-blue-400 mt-1">
                {leaveReport?.totalDaysApproved ?? 0}d
              </p>
            </Card>
          </div>

          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Employee</th>
                      <th className="px-5 py-3">Type</th>
                      <th className="px-5 py-3">Dates</th>
                      <th className="px-5 py-3">Days</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Reviewer</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {(leaveReport?.requests || []).map((r) => (
                      <tr key={r.id} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a]">
                        <td className="px-5 py-3.5">
                          <div className="font-semibold text-neutral-900 dark:text-neutral-100">{r.employeeName}</div>
                          <div className="text-[11px] text-neutral-400">{r.employeeEmail}</div>
                        </td>
                        <td className="px-5 py-3.5">{r.leaveType.replace('_', ' ')}</td>
                        <td className="px-5 py-3.5 whitespace-nowrap">{formatDate(r.startDate)} – {formatDate(r.endDate)}</td>
                        <td className="px-5 py-3.5 font-bold">{r.daysCount}d</td>
                        <td className="px-5 py-3.5">
                          <Badge variant={r.status === 'APPROVED' ? 'success' : r.status === 'PENDING' ? 'warning' : 'default'} size="sm">
                            {r.status}
                          </Badge>
                        </td>
                        <td className="px-5 py-3.5 text-neutral-400">{r.reviewerName || '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};
