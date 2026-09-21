import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Calendar,
  CheckSquare,
  Clock,
  FileStack,
  Layers,
  RefreshCw,
  TrendingUp,
  UserCheck,
  Users,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Button } from '../../components/common/Button';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { PageHeader } from '../../components/layout/PageHeader';
import { StatCard } from '../../components/widgets/StatCard';
import { SimpleBarChart, SimpleDonutChart } from '../../components/widgets/SimpleChart';
import { RecentActivityWidget } from '../../components/widgets/RecentActivityWidget';
import { TaskListWidget } from '../../components/widgets/TaskListWidget';
import { LeaveSummaryWidget } from '../../components/widgets/LeaveSummaryWidget';
import { WorkloadWidget } from '../../components/widgets/WorkloadWidget';
import { WidgetLibraryDrawer } from '../../components/widgets/WidgetLibraryDrawer';
import {
  DEFAULT_DASHBOARD_WIDGETS,
  type WidgetConfig,
} from '../../components/widgets/widgetDefaults';
import {
  MOCK_CLAIMS,
  MOCK_DASHBOARD_KPIS,
} from '../../mocks/mockHrmData';
import { Badge } from '../../components/common/Badge';
import { formatCurrency, formatDate } from '../../utils/formatters';
import { projectsApi, type ProjectStats } from '../../api/projectsApi';

const DASHBOARD_STORAGE_KEY = 'saas_dashboard_widgets_v1';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();
  const { showToast } = useToast();

  const [isWidgetDrawerOpen, setIsWidgetDrawerOpen] = useState(false);
  const [widgets, setWidgets] = useState<WidgetConfig[]>(() => {
    const saved = localStorage.getItem(DASHBOARD_STORAGE_KEY);
    if (!saved) return DEFAULT_DASHBOARD_WIDGETS;
    try {
      const parsed = JSON.parse(saved) as { id: string; visible: boolean }[];
      return DEFAULT_DASHBOARD_WIDGETS.map((w) => {
        const item = parsed.find((p) => p.id === w.id);
        return item ? { ...w, visible: item.visible } : w;
      });
    } catch {
      return DEFAULT_DASHBOARD_WIDGETS;
    }
  });

  const [projectStats, setProjectStats] = useState<ProjectStats | null>(null);

  useEffect(() => {
    projectsApi.getProjectStats()
      .then(setProjectStats)
      .catch(() => {});
  }, []);

  const handleToggleWidget = (widgetId: string) => {
    setWidgets((prev) =>
      prev.map((w) => (w.id === widgetId ? { ...w, visible: !w.visible } : w))
    );
  };

  const handleSaveLayout = () => {
    const serialized = widgets.map((w) => ({ id: w.id, visible: w.visible }));
    localStorage.setItem(DASHBOARD_STORAGE_KEY, JSON.stringify(serialized));
    showToast('success', 'Layout Saved', 'Dashboard customization saved to local storage.');
  };

  const handleResetLayout = () => {
    setWidgets(DEFAULT_DASHBOARD_WIDGETS);
    localStorage.removeItem(DASHBOARD_STORAGE_KEY);
    showToast('info', 'Layout Reset', 'Restored default dashboard layout.');
  };

  const isVisible = (id: string) => widgets.find((w) => w.id === id)?.visible ?? true;

  // Chart data
  const monthlyHoursData = [
    { label: 'Apr', value: 5800 },
    { label: 'May', value: 6100 },
    { label: 'Jun', value: 5950 },
    { label: 'Jul', value: 6300 },
    { label: 'Aug', value: 6250 },
    { label: 'Sep', value: 6420 },
  ];

  const attendanceSegments = [
    { label: 'Present / Active', value: 44, color: '#16a34a', darkColor: '#22c55e' },
    { label: 'Approved Leave', value: 3, color: '#d97706', darkColor: '#f59e0b' },
    { label: 'Probation', value: 1, color: '#71717a', darkColor: '#a1a1aa' },
  ];

  const userName = user?.email ? user.email.split('@')[0] : 'Admin';

  return (
    <div className="space-y-6">
      <PageHeader
        title="Operations Dashboard"
        description={`Welcome back, ${userName}. Here is the real-time operational overview for tenant "${user?.tenantId}".`}
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsWidgetDrawerOpen(true)}
              leftIcon={<Layers className="w-4 h-4" />}
            >
              Customize Widgets
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={handleSaveLayout}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Save Layout
            </Button>
          </div>
        }
      />

      {/* KPI Cards */}
      {isVisible('kpis') && (
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4 sm:gap-5">
          <StatCard
            title="Total Employees"
            value={MOCK_DASHBOARD_KPIS.totalEmployees}
            change={{ value: '+4.2%', isPositive: true, periodText: 'vs last mo' }}
            icon={<Users className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
            iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
          />
          <StatCard
            title="Active Today"
            value={MOCK_DASHBOARD_KPIS.activeEmployees}
            change={{ value: '91.6%', isPositive: true, periodText: 'utilization' }}
            icon={<UserCheck className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />}
            iconBgColor="bg-emerald-50 dark:bg-emerald-950/40"
          />
          <StatCard
            title="Staff On Leave"
            value={MOCK_DASHBOARD_KPIS.onLeave}
            change={{ value: '2 pending', isPositive: false }}
            icon={<Calendar className="w-5 h-5 text-amber-600 dark:text-amber-400" />}
            iconBgColor="bg-amber-50 dark:bg-amber-950/40"
          />
          <StatCard
            title="Hours Logged"
            value={`${MOCK_DASHBOARD_KPIS.hoursLoggedThisMonth}h`}
            change={{ value: '+8.1%', isPositive: true, periodText: 'this month' }}
            icon={<Clock className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
            iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
          />
          <StatCard
            title="Open Claims"
            value={MOCK_DASHBOARD_KPIS.openClaims}
            change={{ value: '$8,420', isPositive: false, periodText: 'value' }}
            icon={<FileStack className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
            iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
          />
          <StatCard
            title="Pending Tasks"
            value={projectStats ? projectStats.openTasks : 0}
            change={{
              value: projectStats ? `${projectStats.overdueTasks} overdue` : '0 overdue',
              isPositive: projectStats ? projectStats.overdueTasks === 0 : true,
            }}
            icon={<CheckSquare className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
            iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
          />
        </div>
      )}

      {/* Main Grid: Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5 sm:gap-6">
        {isVisible('hoursChart') && (
          <Card className="lg:col-span-2 min-w-0 overflow-hidden flex flex-col justify-between">
            <CardHeader className="flex items-center justify-between gap-3 flex-wrap">
              <div className="min-w-0 flex-1">
                <CardTitle className="flex items-center gap-2 min-w-0 text-sm font-semibold">
                  <TrendingUp className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
                  <span>Workforce Hours Logged (Past 6 Months)</span>
                </CardTitle>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                  Monthly billable and operations hours tracking
                </p>
              </div>
              <Badge variant="primary" size="sm" className="shrink-0">
                +12% YoY
              </Badge>
            </CardHeader>
            <CardContent className="p-4 sm:p-5 pt-2 sm:pt-2">
              <SimpleBarChart
                data={monthlyHoursData}
                height={220}
                valueFormatter={(v) => `${v.toLocaleString()} hrs`}
              />
            </CardContent>
          </Card>
        )}

        {isVisible('attendanceChart') && (
          <Card className="min-w-0 overflow-hidden flex flex-col justify-between">
            <CardHeader className="flex items-center justify-between gap-3 flex-wrap">
              <div className="min-w-0 flex-1">
                <CardTitle className="flex items-center gap-2 min-w-0 text-sm font-semibold">
                  <Users className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
                  <span>Attendance Distribution</span>
                </CardTitle>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                  Active roster status for current pay period
                </p>
              </div>
              <Badge variant="default" size="sm" className="shrink-0">
                48 Staff
              </Badge>
            </CardHeader>
            <CardContent className="p-4 sm:p-5 flex items-center justify-center">
              <SimpleDonutChart
                segments={attendanceSegments}
                centerText="48"
                centerSubtext="Employees"
                size={170}
              />
            </CardContent>
          </Card>
        )}
      </div>

      {/* Operations Grid: Tasks & Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 sm:gap-6">
        {isVisible('tasksList') && <TaskListWidget />}
        {isVisible('recentActivity') && <RecentActivityWidget />}
      </div>

      {/* Workforce & Operations Extensions: Leave & Workload */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 sm:gap-6">
        {isVisible('leaveWidget') && <LeaveSummaryWidget />}
        {isVisible('workloadWidget') && <WorkloadWidget />}
      </div>

      {/* Claims Overview Table */}
      {isVisible('claimsSummary') && (
        <Card className="min-w-0 overflow-hidden">
          <CardHeader className="flex items-center justify-between gap-3 flex-wrap">
            <div className="min-w-0 flex-1">
              <CardTitle className="flex items-center gap-2 min-w-0 text-sm font-semibold">
                <FileStack className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
                <span>Recent Expense & Equipment Claims</span>
              </CardTitle>
              <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                Pending and approved employee reimbursement submissions
              </p>
            </div>
            <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
              <Badge variant="default" size="sm">
                {MOCK_CLAIMS.length} Items
              </Badge>
              <Link
                to="/app/claims"
                className="inline-flex items-center gap-1 text-xs font-medium text-neutral-600 hover:text-neutral-950 dark:text-neutral-400 dark:hover:text-neutral-100 transition-colors group"
              >
                <span>See all</span>
                <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
              </Link>
            </div>
          </CardHeader>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                  <tr>
                    <th className="px-5 py-3 whitespace-nowrap">Claim ID</th>
                    <th className="px-5 py-3 whitespace-nowrap">Claimant</th>
                    <th className="px-5 py-3 whitespace-nowrap">Category</th>
                    <th className="px-5 py-3 whitespace-nowrap">Amount</th>
                    <th className="px-5 py-3 whitespace-nowrap">Date</th>
                    <th className="px-5 py-3 whitespace-nowrap">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                  {MOCK_CLAIMS.map((claim) => (
                    <tr
                      key={claim.id}
                      className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                    >
                      <td className="px-5 py-3.5 font-mono font-semibold text-neutral-900 dark:text-neutral-100 whitespace-nowrap">
                        {claim.claimNumber}
                      </td>
                      <td className="px-5 py-3.5 font-medium text-neutral-900 dark:text-neutral-100 whitespace-nowrap">
                        {claim.claimant}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">{claim.category}</td>
                      <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100 whitespace-nowrap">
                        {formatCurrency(claim.amount * 100)}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">{formatDate(claim.dateFiled)}</td>
                      <td className="px-5 py-3.5 whitespace-nowrap">
                        <Badge
                          variant={
                            claim.status === 'Approved'
                              ? 'success'
                              : claim.status === 'Under Review'
                              ? 'warning'
                              : 'info'
                          }
                          size="sm"
                        >
                          {claim.status}
                        </Badge>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Widget Library Drawer */}
      <WidgetLibraryDrawer
        isOpen={isWidgetDrawerOpen}
        onClose={() => setIsWidgetDrawerOpen(false)}
        widgets={widgets}
        onToggleWidget={handleToggleWidget}
        onSaveLayout={handleSaveLayout}
        onResetLayout={handleResetLayout}
      />
    </div>
  );
};
