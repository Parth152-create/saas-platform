import React, { useEffect, useState, useCallback } from 'react';
import {
  Activity,
  Building2,
  Filter,
  Layers,
  RefreshCw,
} from 'lucide-react';
import { useToast } from '../../context/ToastContext';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { Tabs } from '../../components/common/Tabs';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { Modal } from '../../components/common/Modal';
import { Select } from '../../components/common/Select';
import { workloadApi } from '../../api/workloadApi';
import { bulkApi } from '../../api/bulkApi';
import type {
  EmployeeWorkload,
  WorkforceWorkloadSummary,
} from '../../api/types';

export const WorkloadPage: React.FC = () => {
  const { showToast } = useToast();

  const [summary, setSummary] = useState<WorkforceWorkloadSummary | null>(null);
  const [myWorkload, setMyWorkload] = useState<EmployeeWorkload | null>(null);
  const [activeTab, setActiveTab] = useState<'team' | 'department' | 'project' | 'my'>('team');
  const [departmentFilter, setDepartmentFilter] = useState<string>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  // Bulk modal state
  const [isBulkModalOpen, setIsBulkModalOpen] = useState(false);
  const [bulkTaskIdsInput, setBulkTaskIdsInput] = useState('');
  const [bulkTargetStatus, setBulkTargetStatus] = useState('IN_PROGRESS');
  const [isSubmittingBulk, setIsSubmittingBulk] = useState(false);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const [sum, my] = await Promise.all([
        workloadApi.getSummary(),
        workloadApi.getMyWorkload().catch(() => null),
      ]);
      setSummary(sum);
      setMyWorkload(my);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load workload analytics';
      showToast('error', 'Error', msg);
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      workloadApi.getSummary(),
      workloadApi.getMyWorkload().catch(() => null),
    ])
      .then(([sum, my]) => {
        if (isMounted) {
          setSummary(sum);
          setMyWorkload(my);
        }
      })
      .catch((err: unknown) => {
        if (isMounted) {
          const msg = err instanceof Error ? err.message : 'Failed to load workload analytics';
          showToast('error', 'Error', msg);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [showToast]);

  const handleRunBulkStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    const ids = bulkTaskIdsInput
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    if (ids.length === 0) {
      showToast('warning', 'No IDs', 'Please enter at least one task ID.');
      return;
    }

    try {
      setIsSubmittingBulk(true);
      const res = await bulkApi.updateTaskStatus(ids, bulkTargetStatus);
      showToast(
        'success',
        'Bulk Status Update',
        `Successfully updated ${res.successCount} of ${res.totalRequested} tasks.`
      );
      if (res.failureCount > 0) {
        showToast('warning', 'Partial Failures', `${res.failureCount} tasks could not be updated.`);
      }
      setIsBulkModalOpen(false);
      setBulkTaskIdsInput('');
      loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Bulk update failed';
      showToast('error', 'Bulk Update Failed', msg);
    } finally {
      setIsSubmittingBulk(false);
    }
  };

  const getUtilizationBarColor = (pct: number) => {
    if (pct > 100) return 'bg-red-500 dark:bg-red-400';
    if (pct >= 80) return 'bg-amber-500 dark:bg-amber-400';
    return 'bg-emerald-500 dark:bg-emerald-400';
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'OVERLOADED':
        return <Badge variant="danger" size="sm" withDot>Overloaded</Badge>;
      case 'HIGH':
        return <Badge variant="warning" size="sm" withDot>Near Capacity</Badge>;
      case 'OPTIMAL':
      case 'NORMAL':
        return <Badge variant="success" size="sm" withDot>Optimal</Badge>;
      default:
        return <Badge size="sm">{status}</Badge>;
    }
  };

  const employeeWorkloads = summary?.employeeWorkloads || [];
  const departmentWorkloads = summary?.departmentWorkloads || [];
  const projectWorkloads = summary?.projectWorkloads || [];

  const filteredEmployees = employeeWorkloads.filter((e) => {
    if (departmentFilter !== 'ALL' && e.department !== departmentFilter) return false;
    return true;
  });

  const overloadedCount = employeeWorkloads.filter((e) => e.capacityUtilization > 100).length;
  const availableCount = employeeWorkloads.filter((e) => e.capacityUtilization < 70).length;

  const departmentsList = Array.from(new Set(employeeWorkloads.map((e) => e.department))).filter(Boolean);

  const tabs = [
    { id: 'team', label: 'Team Workload', count: employeeWorkloads.length },
    { id: 'department', label: 'Department Breakdown', count: departmentWorkloads.length },
    { id: 'project', label: 'Project Workload', count: projectWorkloads.length },
    ...(myWorkload ? [{ id: 'my', label: 'My Workload' }] : []),
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Workload &amp; Capacity Planner"
        description="Monitor workforce task load, weekly capacity utilization, bottlenecks, and project distributions."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={loadData}
              leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
            >
              Sync
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setIsBulkModalOpen(true)}
              leftIcon={<Layers className="w-3.5 h-3.5" />}
            >
              Bulk Task Operations
            </Button>
          </div>
        }
      />

      {/* KPI Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4">
        {isLoading && !summary ? (
          Array.from({ length: 6 }).map((_, i) => (
            <LoadingSkeleton key={i} className="h-24 rounded-xl" />
          ))
        ) : (
          <>
            <Card className="p-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-neutral-400">Total Workforce</span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {summary?.totalEmployees ?? 0}
                </span>
                <span className="text-xs text-neutral-400">staff</span>
              </div>
            </Card>

            <Card className="p-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-neutral-400">Active Tasks</span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {summary?.openTasks ?? 0}
                </span>
                <span className="text-xs text-neutral-400">open</span>
              </div>
            </Card>

            <Card className="p-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-neutral-400">Estimated Hours</span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {Math.round(summary?.totalEstimatedHours ?? 0)}h
                </span>
                <span className="text-xs text-neutral-400">demand</span>
              </div>
            </Card>

            <Card className="p-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-neutral-400">Avg Utilization</span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {Math.round(summary?.averageUtilizationPercentage ?? 0)}%
                </span>
                <span className="text-xs text-neutral-400">capacity</span>
              </div>
            </Card>

            <Card className="p-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-amber-500">Overloaded Staff</span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-2xl font-bold text-red-600 dark:text-red-400">
                  {overloadedCount}
                </span>
                <span className="text-xs text-neutral-400">&gt;100% cap</span>
              </div>
            </Card>

            <Card className="p-4">
              <span className="text-[11px] font-bold uppercase tracking-wider text-emerald-500">Available Staff</span>
              <div className="flex items-baseline gap-1 mt-1">
                <span className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                  {availableCount}
                </span>
                <span className="text-xs text-neutral-400">&lt;70% cap</span>
              </div>
            </Card>
          </>
        )}
      </div>

      {/* Tabs and Department Filter */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-neutral-200 dark:border-[#262626] pb-1">
        <Tabs
          tabs={tabs}
          activeTab={activeTab}
          onChange={(tab) => setActiveTab(tab as 'team' | 'department' | 'project' | 'my')}
        />

        {activeTab === 'team' && (
          <div className="flex items-center gap-2">
            <Filter className="w-3.5 h-3.5 text-neutral-400" />
            <select
              value={departmentFilter}
              onChange={(e) => setDepartmentFilter(e.target.value)}
              className="text-xs rounded-lg border border-neutral-200 bg-white dark:bg-[#141414] dark:border-[#262626] px-2.5 py-1 text-neutral-700 dark:text-neutral-300"
            >
              <option value="ALL">All Departments</option>
              {departmentsList.map((dep) => (
                <option key={dep} value={dep}>
                  {dep}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>

      {/* Tab 1: Team Workload Table */}
      {activeTab === 'team' && (
        <Card>
          <CardContent className="p-0">
            {filteredEmployees.length === 0 ? (
              <div className="p-12 text-center text-xs text-neutral-500">
                No employees matching department filter.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Employee</th>
                      <th className="px-5 py-3">Department</th>
                      <th className="px-5 py-3">Active Tasks</th>
                      <th className="px-5 py-3">Est. Remaining</th>
                      <th className="px-5 py-3">Weekly Capacity</th>
                      <th className="px-5 py-3 min-w-[180px]">Capacity Utilization</th>
                      <th className="px-5 py-3">Status</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {filteredEmployees.map((emp) => {
                      const utilPct = Math.round(emp.capacityUtilization);
                      return (
                        <tr key={emp.employeeOrUserId} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors">
                          <td className="px-5 py-3.5">
                            <div className="font-semibold text-neutral-900 dark:text-neutral-100">
                              {emp.name}
                            </div>
                            <div className="text-[11px] text-neutral-400 font-mono">
                              {emp.email}
                            </div>
                          </td>
                          <td className="px-5 py-3.5 whitespace-nowrap">
                            {emp.department || 'General'}
                          </td>
                          <td className="px-5 py-3.5">
                            <span className="font-bold text-neutral-900 dark:text-neutral-100">
                              {emp.openTasks}
                            </span>
                            <span className="text-neutral-400 text-[11px] ml-1">
                              ({emp.totalTasks} total)
                            </span>
                            {emp.overdueTasks > 0 && (
                              <Badge variant="danger" size="sm" className="ml-2">
                                {emp.overdueTasks} overdue
                              </Badge>
                            )}
                          </td>
                          <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                            {emp.estimatedHoursRemaining} hrs
                          </td>
                          <td className="px-5 py-3.5 text-neutral-500">
                            {emp.weeklyCapacityHours} hrs/wk
                          </td>
                          <td className="px-5 py-3.5">
                            <div className="space-y-1">
                              <div className="flex justify-between text-[11px] font-semibold">
                                <span className={utilPct > 100 ? 'text-red-600 dark:text-red-400' : 'text-neutral-800 dark:text-neutral-200'}>
                                  {utilPct}%
                                </span>
                                <span className="text-neutral-400">
                                  {emp.estimatedHoursRemaining}/{emp.weeklyCapacityHours}h
                                </span>
                              </div>
                              <div className="w-full bg-neutral-200 dark:bg-[#262626] rounded-full h-2 overflow-hidden">
                                <div
                                  className={`h-2 rounded-full transition-all duration-300 ${getUtilizationBarColor(utilPct)}`}
                                  style={{ width: `${Math.min(utilPct, 100)}%` }}
                                />
                              </div>
                            </div>
                          </td>
                          <td className="px-5 py-3.5 whitespace-nowrap">
                            {getStatusBadge(emp.workloadStatus)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab 2: Department Breakdown */}
      {activeTab === 'department' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {departmentWorkloads.map((dep) => (
            <Card key={dep.department} className="flex flex-col justify-between">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-bold flex items-center justify-between">
                  <span className="flex items-center gap-2">
                    <Building2 className="w-4 h-4 text-neutral-700 dark:text-neutral-300" />
                    {dep.department || 'Unassigned'}
                  </span>
                  <Badge variant="primary" size="sm">
                    {dep.employeeCount} Staff
                  </Badge>
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-1">
                  <div className="flex justify-between text-xs font-semibold">
                    <span className="text-neutral-500">Utilization:</span>
                    <span className={dep.averageUtilization > 100 ? 'text-red-600 font-bold' : 'text-neutral-900 dark:text-neutral-100'}>
                      {Math.round(dep.averageUtilization)}%
                    </span>
                  </div>
                  <div className="w-full bg-neutral-200 dark:bg-[#262626] rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-2 rounded-full ${getUtilizationBarColor(dep.averageUtilization)}`}
                      style={{ width: `${Math.min(dep.averageUtilization, 100)}%` }}
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-neutral-100 dark:border-[#262626] text-xs">
                  <div>
                    <span className="text-neutral-400">Open Tasks:</span>
                    <p className="font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">{dep.openTasks}</p>
                  </div>
                  <div>
                    <span className="text-neutral-400">Overdue:</span>
                    <p className="font-bold text-red-600 dark:text-red-400 mt-0.5">{dep.overdueTasks}</p>
                  </div>
                  <div>
                    <span className="text-neutral-400">Est. Hours:</span>
                    <p className="font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">{Math.round(dep.totalEstimatedHours)}h</p>
                  </div>
                  <div>
                    <span className="text-neutral-400">Actual Hours:</span>
                    <p className="font-bold text-neutral-900 dark:text-neutral-100 mt-0.5">{Math.round(dep.totalActualHours)}h</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Tab 3: Project Workload */}
      {activeTab === 'project' && (
        <Card>
          <CardContent className="p-0">
            {projectWorkloads.length === 0 ? (
              <div className="p-12 text-center text-xs text-neutral-500">
                No active projects found.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
                  <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                    <tr>
                      <th className="px-5 py-3">Project Name</th>
                      <th className="px-5 py-3">Status</th>
                      <th className="px-5 py-3">Open / Total Tasks</th>
                      <th className="px-5 py-3">Overdue</th>
                      <th className="px-5 py-3">Hours (Est / Actual)</th>
                      <th className="px-5 py-3 min-w-[160px]">Progress</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                    {projectWorkloads.map((proj) => (
                      <tr key={proj.projectId} className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors">
                        <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                          {proj.projectName}
                        </td>
                        <td className="px-5 py-3.5">
                          <Badge variant="default" size="sm">{proj.status}</Badge>
                        </td>
                        <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                          {proj.openTasks} / {proj.totalTasks}
                        </td>
                        <td className="px-5 py-3.5">
                          {proj.overdueTasks > 0 ? (
                            <Badge variant="danger" size="sm">{proj.overdueTasks} overdue</Badge>
                          ) : (
                            <span className="text-neutral-400">0</span>
                          )}
                        </td>
                        <td className="px-5 py-3.5 font-mono text-[11px]">
                          {proj.estimatedHours}h / {proj.actualHours}h
                        </td>
                        <td className="px-5 py-3.5">
                          <div className="space-y-1">
                            <span className="text-[11px] font-semibold text-neutral-800 dark:text-neutral-200">
                              {proj.progressPercentage}%
                            </span>
                            <div className="w-full bg-neutral-200 dark:bg-[#262626] rounded-full h-2 overflow-hidden">
                              <div
                                className="h-2 rounded-full bg-neutral-900 dark:bg-white"
                                style={{ width: `${proj.progressPercentage}%` }}
                              />
                            </div>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Tab 4: My Workload */}
      {activeTab === 'my' && myWorkload && (
        <div className="max-w-2xl mx-auto space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-sm font-bold flex items-center gap-2">
                <Activity className="w-4 h-4 text-neutral-900 dark:text-neutral-100" />
                <span>My Workload &amp; Capacity Profile</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between p-4 bg-neutral-50 dark:bg-[#181818] rounded-xl">
                <div>
                  <h4 className="text-base font-bold text-neutral-900 dark:text-neutral-100">
                    {myWorkload.name}
                  </h4>
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    {myWorkload.department} • {myWorkload.email}
                  </p>
                </div>
                {getStatusBadge(myWorkload.workloadStatus)}
              </div>

              <div className="space-y-2">
                <div className="flex justify-between text-xs font-semibold">
                  <span>Current Utilization</span>
                  <span className="font-bold">{Math.round(myWorkload.capacityUtilization)}%</span>
                </div>
                <div className="w-full bg-neutral-200 dark:bg-[#262626] rounded-full h-3 overflow-hidden">
                  <div
                    className={`h-3 rounded-full ${getUtilizationBarColor(myWorkload.capacityUtilization)}`}
                    style={{ width: `${Math.min(myWorkload.capacityUtilization, 100)}%` }}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4 border-t border-neutral-100 dark:border-[#262626] text-center">
                <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg">
                  <span className="text-[10px] uppercase font-bold text-neutral-400">Open Tasks</span>
                  <p className="text-xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">{myWorkload.openTasks}</p>
                </div>
                <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg">
                  <span className="text-[10px] uppercase font-bold text-neutral-400">Remaining</span>
                  <p className="text-xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">{myWorkload.estimatedHoursRemaining}h</p>
                </div>
                <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg">
                  <span className="text-[10px] uppercase font-bold text-neutral-400">Weekly Cap</span>
                  <p className="text-xl font-bold text-neutral-900 dark:text-neutral-100 mt-1">{myWorkload.weeklyCapacityHours}h</p>
                </div>
                <div className="p-3 bg-neutral-50 dark:bg-[#141414] rounded-lg">
                  <span className="text-[10px] uppercase font-bold text-neutral-400">Overdue</span>
                  <p className="text-xl font-bold text-red-600 dark:text-red-400 mt-1">{myWorkload.overdueTasks}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Bulk Operations Modal */}
      <Modal
        isOpen={isBulkModalOpen}
        onClose={() => setIsBulkModalOpen(false)}
        title="Execute Bulk Task Update"
        description="Update multiple task statuses at once. Provide comma-separated UUIDs."
        footer={
          <>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setIsBulkModalOpen(false)}
              disabled={isSubmittingBulk}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              onClick={handleRunBulkStatus}
              isLoading={isSubmittingBulk}
            >
              Update Tasks
            </Button>
          </>
        }
      >
        <form onSubmit={handleRunBulkStatus} className="space-y-4">
          <Select
            label="Target Status"
            value={bulkTargetStatus}
            onChange={(e) => setBulkTargetStatus(e.target.value)}
            options={[
              { value: 'BACKLOG', label: 'Backlog' },
              { value: 'TODO', label: 'To Do' },
              { value: 'IN_PROGRESS', label: 'In Progress' },
              { value: 'IN_REVIEW', label: 'In Review' },
              { value: 'DONE', label: 'Done' },
            ]}
          />

          <div>
            <label className="block text-xs font-semibold text-neutral-700 dark:text-neutral-300 uppercase tracking-wider mb-1.5">
              Task IDs (Comma-Separated)
            </label>
            <textarea
              rows={3}
              value={bulkTaskIdsInput}
              onChange={(e) => setBulkTaskIdsInput(e.target.value)}
              placeholder="e.g. 550e8400-e29b-41d4-a716-446655440000, a1b2c3d4-..."
              className="w-full rounded-lg border border-neutral-300 dark:border-[#262626] bg-white dark:bg-[#141414] px-3 py-2 text-xs font-mono text-neutral-900 dark:text-neutral-100 placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-neutral-900/10 focus:border-neutral-900"
              required
            />
          </div>
        </form>
      </Modal>
    </div>
  );
};
