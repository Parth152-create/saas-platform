import React, { useState } from 'react';
import { Calendar, Check, Clock, Plus, UserCheck, X } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { StatCard } from '../../components/widgets/StatCard';
import { MOCK_ABSENCE_TYPES } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const AttendanceLeavePage: React.FC = () => {
  const { showToast } = useToast();

  const [leaveRequests, setLeaveRequests] = useState([
    {
      id: 'lr-1',
      employee: 'Marcus Sterling',
      type: 'Paid Time Off',
      dates: 'Oct 4, 2026 - Oct 7, 2026 (3 days)',
      reason: 'Family event & travel',
      status: 'PENDING',
    },
    {
      id: 'lr-2',
      employee: 'Elena Rostova',
      type: 'Parental Leave',
      dates: 'Nov 1, 2026 - Dec 15, 2026 (30 days)',
      reason: 'Maternity leave',
      status: 'APPROVED',
    },
    {
      id: 'lr-3',
      employee: 'David Okafor',
      type: 'Sick & Medical',
      dates: 'Sep 10, 2026 - Sep 11, 2026 (1 day)',
      reason: 'Dental surgery',
      status: 'APPROVED',
    },
  ]);

  const handleDecision = (id: string, status: 'APPROVED' | 'REJECTED') => {
    setLeaveRequests((prev) =>
      prev.map((r) => (r.id === id ? { ...r, status } : r))
    );
    showToast('success', `Request ${status.toLowerCase()}`, 'Status updated in workforce schedule.');
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Attendance & Leave Management"
        description="Monitor organization punch logs, review time-off requests, and configure absence policy quotas."
        actions={
          <Button
            size="sm"
            onClick={() => showToast('info', 'Request Leave', 'Time-off submission dialog opened')}
            leftIcon={<Plus className="w-4 h-4" />}
          >
            Submit Leave Request
          </Button>
        }
      />

      {/* KPI Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Attendance Rate"
          value="98.2%"
          change={{ value: '+0.5%', isPositive: true }}
          icon={<UserCheck className="w-5 h-5 text-emerald-600" />}
          iconBgColor="bg-emerald-50 dark:bg-emerald-950/60"
        />
        <StatCard
          title="Staff On Leave Today"
          value="3"
          icon={<Calendar className="w-5 h-5 text-amber-600" />}
          iconBgColor="bg-amber-50 dark:bg-amber-950/60"
        />
        <StatCard
          title="Pending Approvals"
          value={leaveRequests.filter((r) => r.status === 'PENDING').length}
          icon={<Clock className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
          iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
        />
        <StatCard
          title="Total Absence Types"
          value={MOCK_ABSENCE_TYPES.length}
          icon={<Calendar className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
          iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
        />
      </div>

      {/* Leave Requests Table */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Time-Off & Leave Requests</CardTitle>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
              Action pending leaves and track schedule impacts
            </p>
          </div>
          <Badge variant="warning" size="sm">
            {leaveRequests.filter((r) => r.status === 'PENDING').length} Action Required
          </Badge>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Employee</th>
                  <th className="px-5 py-3">Absence Type</th>
                  <th className="px-5 py-3">Dates Requested</th>
                  <th className="px-5 py-3">Reason / Notes</th>
                  <th className="px-5 py-3">Status</th>
                  <th className="px-5 py-3 text-right">Decision</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {leaveRequests.map((req) => (
                  <tr
                    key={req.id}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                  >
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                      {req.employee}
                    </td>
                    <td className="px-5 py-3.5 font-medium">{req.type}</td>
                    <td className="px-5 py-3.5">{req.dates}</td>
                    <td className="px-5 py-3.5 text-neutral-500 italic">{req.reason}</td>
                    <td className="px-5 py-3.5">
                      <Badge
                        variant={
                          req.status === 'APPROVED'
                            ? 'success'
                            : req.status === 'PENDING'
                            ? 'warning'
                            : 'danger'
                        }
                        size="sm"
                        withDot
                      >
                        {req.status}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      {req.status === 'PENDING' ? (
                        <div className="flex items-center justify-end gap-1.5">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleDecision(req.id, 'APPROVED')}
                            leftIcon={<Check className="w-3.5 h-3.5 text-emerald-600" />}
                            className="text-emerald-700 dark:text-emerald-300 border-emerald-300"
                          >
                            Approve
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleDecision(req.id, 'REJECTED')}
                            leftIcon={<X className="w-3.5 h-3.5 text-red-600" />}
                            className="text-red-700 dark:text-red-300 border-red-300"
                          >
                            Reject
                          </Button>
                        </div>
                      ) : (
                        <span className="text-[11px] text-neutral-400 font-medium">Decided</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Absence Types Quotas */}
      <Card>
        <CardHeader>
          <CardTitle>Configured Organization Absence Quotas</CardTitle>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
            Standard leave policies assigned to company employees
          </p>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-400 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Absence Policy Name</th>
                  <th className="px-5 py-3">Annual Allowance</th>
                  <th className="px-5 py-3">Compensation</th>
                  <th className="px-5 py-3">Manager Approval</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {MOCK_ABSENCE_TYPES.map((abs) => (
                  <tr key={abs.id}>
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                      {abs.name}
                    </td>
                    <td className="px-5 py-3.5 font-medium">{abs.allowanceDays} days/yr</td>
                    <td className="px-5 py-3.5">
                      <Badge variant={abs.paid ? 'success' : 'default'} size="sm">
                        {abs.paid ? 'Fully Paid' : 'Unpaid'}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5">
                      {abs.requiresApproval ? (
                        <span className="text-neutral-700 dark:text-neutral-300 font-medium">
                          Required
                        </span>
                      ) : (
                        <span className="text-neutral-400">Auto-logged</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};
