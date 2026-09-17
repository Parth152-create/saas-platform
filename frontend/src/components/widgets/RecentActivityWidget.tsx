import React from 'react';
import { Link } from 'react-router-dom';
import { Clock, ShieldCheck, UserCheck, CheckCircle2, ArrowRight } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '../common/Card';
import type { ActivityItem } from '../../mocks/mockHrmData';

export interface RecentActivityWidgetProps {
  activities: ActivityItem[];
  className?: string;
  seeAllLink?: string;
}

export const RecentActivityWidget: React.FC<RecentActivityWidgetProps> = ({
  activities,
  className = '',
  seeAllLink = '/app/reports',
}) => {
  return (
    <Card className={`overflow-hidden min-w-0 ${className}`}>
      <CardHeader className="flex items-center justify-between gap-2.5 sm:gap-4 flex-wrap">
        <div className="flex items-center gap-2 min-w-0 flex-1">
          <Clock className="w-4 h-4 text-neutral-900 dark:text-neutral-100 shrink-0" />
          <CardTitle className="text-sm font-semibold">Recent Activity & Audit</CardTitle>
        </div>
        <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
          <span className="text-[11px] font-semibold text-neutral-500 dark:text-neutral-400">
            Live Updates
          </span>
          <Link
            to={seeAllLink}
            className="inline-flex items-center gap-1 text-xs font-medium text-neutral-600 hover:text-neutral-950 dark:text-neutral-400 dark:hover:text-neutral-100 transition-colors group"
          >
            <span>See all</span>
            <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
          </Link>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y divide-neutral-100 dark:divide-[#262626]">
          {activities.map((item) => (
            <div
              key={item.id}
              className="flex items-start gap-3 p-4 hover:bg-neutral-50/50 dark:hover:bg-[#1a1a1a] transition-colors"
            >
              <div className="mt-0.5 flex h-7 w-7 items-center justify-center rounded-full bg-neutral-100 text-neutral-900 dark:bg-[#1f1f1f] dark:text-neutral-100 shrink-0">
                {item.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                ) : item.type === 'warning' ? (
                  <UserCheck className="w-4 h-4 text-amber-500" />
                ) : (
                  <ShieldCheck className="w-4 h-4 text-neutral-700 dark:text-neutral-300" />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-neutral-700 dark:text-neutral-300">
                  <strong className="font-semibold text-neutral-900 dark:text-neutral-100">
                    {item.user}
                  </strong>{' '}
                  {item.action}{' '}
                  <span className="font-medium text-neutral-900 dark:text-neutral-100">
                    &quot;{item.target}&quot;
                  </span>
                </p>
                <span className="text-[10px] text-neutral-400 mt-1 block">{item.timestamp}</span>
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
};
