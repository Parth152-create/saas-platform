import React, { useState } from 'react';
import { Clock, Download, Play, StopCircle, UserCheck } from 'lucide-react';
import { PageHeader } from '../../components/layout/PageHeader';
import { Button } from '../../components/common/Button';
import { Badge } from '../../components/common/Badge';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/common/Card';
import { StatCard } from '../../components/widgets/StatCard';
import { INITIAL_EMPLOYEES } from '../../mocks/mockHrmData';
import { useToast } from '../../context/ToastContext';

export const TimeTrackingPage: React.FC = () => {
  const { showToast } = useToast();
  const [isTimerRunning, setIsTimerRunning] = useState(false);
  const currentTask = 'Sprint Planning & Platform Architecture';

  const toggleTimer = () => {
    setIsTimerRunning(!isTimerRunning);
    showToast(
      isTimerRunning ? 'info' : 'success',
      isTimerRunning ? 'Work Timer Paused' : 'Work Timer Started',
      `Active on "${currentTask}"`
    );
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Time & Productivity Tracking"
        description="Live work tracking, billable efficiency summaries, and employee timesheet audits."
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant={isTimerRunning ? 'danger' : 'primary'}
              size="sm"
              onClick={toggleTimer}
              leftIcon={isTimerRunning ? <StopCircle className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            >
              {isTimerRunning ? 'Stop Clock (01:24:05)' : 'Start Work Clock'}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => showToast('info', 'Exporting CSV', 'Monthly timesheet generated')}
              leftIcon={<Download className="w-4 h-4" />}
            >
              Export Timesheets
            </Button>
          </div>
        }
      />

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <StatCard
          title="Total Hours Logged (Mo)"
          value="6,420 hrs"
          change={{ value: '+4.2% vs target', isPositive: true }}
          icon={<Clock className="w-5 h-5 text-zinc-900 dark:text-neutral-100" />}
        />
        <StatCard
          title="Billable Efficiency"
          value="94.8%"
          change={{ value: 'Target: >90%', isPositive: true }}
          icon={<UserCheck className="w-5 h-5 text-emerald-600" />}
          iconBgColor="bg-emerald-50 dark:bg-emerald-950/60"
        />
        <StatCard
          title="Active Timers Right Now"
          value="38 staff"
          icon={<Clock className="w-5 h-5 text-neutral-900 dark:text-neutral-100" />}
          iconBgColor="bg-neutral-100 dark:bg-[#1f1f1f]"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Staff Work Log Ledger</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-neutral-600 dark:text-neutral-400">
              <thead className="bg-neutral-50 text-[11px] font-bold uppercase tracking-wider text-neutral-400 dark:bg-[#141414] dark:text-neutral-500 border-b border-neutral-100 dark:border-[#262626]">
                <tr>
                  <th className="px-5 py-3">Employee</th>
                  <th className="px-5 py-3">Department</th>
                  <th className="px-5 py-3">Monthly Hours</th>
                  <th className="px-5 py-3">Billable Rate</th>
                  <th className="px-5 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-[#262626]">
                {INITIAL_EMPLOYEES.map((emp) => (
                  <tr
                    key={emp.id}
                    className="hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
                  >
                    <td className="px-5 py-3.5 font-semibold text-neutral-900 dark:text-neutral-100">
                      {emp.name}
                    </td>
                    <td className="px-5 py-3.5">{emp.department}</td>
                    <td className="px-5 py-3.5 font-mono font-medium">{emp.billableHours} hrs</td>
                    <td className="px-5 py-3.5 font-medium text-emerald-600 dark:text-emerald-400">
                      100%
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge variant={emp.status === 'ACTIVE' ? 'success' : 'warning'} size="sm">
                        {emp.status}
                      </Badge>
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
