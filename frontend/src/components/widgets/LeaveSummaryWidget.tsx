import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, ArrowRight, UserCheck } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import { Badge } from '../common/Badge';
import { formatDate } from '../../utils/formatters';
import { leaveApi } from '../../api/leaveApi';
import type { LeaveRequest } from '../../api/types';
import { useAuth } from '../../context/AuthContext';

export const LeaveSummaryWidget: React.FC = () => {
  const { hasRole } = useAuth();
  const isManagerOrAdmin = hasRole('MANAGER') || hasRole('ADMIN') || hasRole('SUPER_ADMIN');

  const [requests, setRequests] = useState<LeaveRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    const fetchCall = isManagerOrAdmin
      ? leaveApi.getPendingLeaveRequests()
      : leaveApi.getMyLeaveRequests();

    fetchCall
      .then((data) => {
        if (isMounted) setRequests(data.slice(0, 5));
      })
      .catch(() => {
        if (isMounted) setRequests([]);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isManagerOrAdmin]);

  return (
    <Card className="overflow-hidden min-w-0">
      <CardHeader className="flex items-center justify-between gap-2.5 sm:gap-4 flex-wrap">
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <Calendar className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
          <CardTitle className="text-sm font-semibold">
            {isManagerOrAdmin ? 'Pending Leave Approvals' : 'My Leave Status'}
          </CardTitle>
        </div>
        <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
          <span className="text-[11px] font-semibold text-neutral-500 dark:text-neutral-400">
            {requests.length} Items
          </span>
          <Link
            to="/app/leave"
            className="inline-flex items-center gap-1 text-xs font-medium text-neutral-600 hover:text-neutral-950 dark:text-neutral-400 dark:hover:text-neutral-100 transition-colors group"
          >
            <span>Manage</span>
            <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        {isLoading ? (
          <div className="p-4 space-y-2">
            <div className="h-4 bg-neutral-100 dark:bg-[#1a1a1a] rounded animate-pulse w-3/4" />
            <div className="h-4 bg-neutral-100 dark:bg-[#1a1a1a] rounded animate-pulse w-1/2" />
          </div>
        ) : requests.length === 0 ? (
          <div className="p-8 text-center text-xs text-neutral-400">
            <UserCheck className="w-6 h-6 mx-auto mb-2 opacity-50" />
            No pending leave requests.
          </div>
        ) : (
          <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
            {requests.map((r) => (
              <div
                key={r.id}
                className="flex items-center justify-between p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors gap-3"
              >
                <div className="space-y-0.5 min-w-0 flex-1">
                  <p className="text-xs font-semibold text-neutral-900 dark:text-neutral-100 truncate">
                    {r.employeeName} — {r.leaveType.replace('_', ' ')}
                  </p>
                  <p className="text-[10px] text-neutral-400 truncate">
                    {formatDate(r.startDate)} – {formatDate(r.endDate)} ({r.daysCount}d)
                  </p>
                </div>
                <Badge
                  variant={
                    r.status === 'APPROVED'
                      ? 'success'
                      : r.status === 'PENDING'
                      ? 'warning'
                      : 'default'
                  }
                  size="sm"
                >
                  {r.status}
                </Badge>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
};
