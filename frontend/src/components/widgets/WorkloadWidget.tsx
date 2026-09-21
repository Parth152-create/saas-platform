import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Activity, ArrowRight } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import { workloadApi } from '../../api/workloadApi';
import type { WorkforceWorkloadSummary } from '../../api/types';

export const WorkloadWidget: React.FC = () => {
  const [summary, setSummary] = useState<WorkforceWorkloadSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    workloadApi
      .getSummary()
      .then((data) => {
        if (isMounted) setSummary(data);
      })
      .catch(() => {
        if (isMounted) setSummary(null);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const overloadedCount = (summary?.employeeWorkloads || []).filter(
    (e) => e.capacityUtilization > 100
  ).length;

  const avgUtilization = Math.round(summary?.averageUtilizationPercentage ?? 0);

  return (
    <Card className="overflow-hidden min-w-0">
      <CardHeader className="flex items-center justify-between gap-2.5 sm:gap-4 flex-wrap">
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <Activity className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
          <CardTitle className="text-sm font-semibold">Workload &amp; Capacity</CardTitle>
        </div>
        <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
          <Link
            to="/app/workload"
            className="inline-flex items-center gap-1 text-xs font-medium text-neutral-600 hover:text-neutral-950 dark:text-neutral-400 dark:hover:text-neutral-100 transition-colors group"
          >
            <span>Capacity Board</span>
            <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>
      </CardHeader>
      <CardContent className="p-4 sm:p-5 pt-2">
        {isLoading ? (
          <div className="space-y-2">
            <div className="h-4 bg-neutral-100 dark:bg-[#1a1a1a] rounded animate-pulse w-3/4" />
            <div className="h-4 bg-neutral-100 dark:bg-[#1a1a1a] rounded animate-pulse w-1/2" />
          </div>
        ) : !summary ? (
          <div className="text-center text-xs text-neutral-400 py-4">
            Workload metrics currently unavailable.
          </div>
        ) : (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <span className="text-xs text-neutral-500">Average Utilization</span>
                <p className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">
                  {avgUtilization}%
                </p>
              </div>
              <div className="text-right">
                <span className="text-xs text-neutral-500">Overloaded Staff</span>
                <p className="text-2xl font-bold text-red-600 dark:text-red-400">
                  {overloadedCount}
                </p>
              </div>
            </div>

            <div className="w-full bg-neutral-200 dark:bg-[#262626] rounded-full h-2.5 overflow-hidden">
              <div
                className={`h-2.5 rounded-full ${
                  avgUtilization > 100
                    ? 'bg-red-500'
                    : avgUtilization >= 80
                    ? 'bg-amber-500'
                    : 'bg-emerald-500'
                }`}
                style={{ width: `${Math.min(avgUtilization, 100)}%` }}
              />
            </div>

            <div className="grid grid-cols-3 gap-2 pt-2 border-t border-neutral-100 dark:border-[#262626] text-center text-[11px]">
              <div>
                <span className="text-neutral-400">Active Tasks</span>
                <p className="font-bold text-neutral-800 dark:text-neutral-200">{summary.openTasks}</p>
              </div>
              <div>
                <span className="text-neutral-400">Est. Hours</span>
                <p className="font-bold text-neutral-800 dark:text-neutral-200">{Math.round(summary.totalEstimatedHours)}h</p>
              </div>
              <div>
                <span className="text-neutral-400">Employees</span>
                <p className="font-bold text-neutral-800 dark:text-neutral-200">{summary.totalEmployees}</p>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
};
