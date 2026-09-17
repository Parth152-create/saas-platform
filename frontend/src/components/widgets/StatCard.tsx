import React from 'react';
import { TrendingDown, TrendingUp } from 'lucide-react';
import { Card } from '../common/Card';

export interface StatCardProps {
  title: string;
  value: string | number;
  change?: {
    value: string;
    isPositive: boolean;
    periodText?: string;
  };
  icon: React.ReactNode;
  iconBgColor?: string;
  className?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  change,
  icon,
  iconBgColor = 'bg-neutral-100 text-neutral-900 dark:bg-[#1f1f1f] dark:text-neutral-100',
  className = '',
}) => {
  return (
    <Card className={`p-4 sm:p-5 flex flex-col justify-between overflow-hidden min-w-0 ${className}`}>
      <div className="flex items-start justify-between gap-2 min-w-0">
        <span
          className="text-xs font-semibold uppercase tracking-wider text-neutral-500 dark:text-neutral-400 truncate"
          title={title}
        >
          {title}
        </span>
        <div className={`flex h-9 w-9 items-center justify-center rounded-xl shrink-0 ${iconBgColor}`}>
          {icon}
        </div>
      </div>

      <div className="mt-3 flex items-baseline justify-between gap-2 flex-wrap min-w-0">
        <span className="text-2xl font-bold tracking-tight text-neutral-900 dark:text-neutral-100 shrink-0">
          {value}
        </span>

        {change && (
          <div
            className={`inline-flex items-center gap-1 text-xs font-semibold shrink-0 ${
              change.isPositive
                ? 'text-emerald-600 dark:text-emerald-400'
                : 'text-red-600 dark:text-red-400'
            }`}
          >
            {change.isPositive ? (
              <TrendingUp className="w-3.5 h-3.5 shrink-0" />
            ) : (
              <TrendingDown className="w-3.5 h-3.5 shrink-0" />
            )}
            <span>{change.value}</span>
            {change.periodText && (
              <span className="text-[10px] text-neutral-400 font-normal hidden sm:inline 2xl:inline">
                {change.periodText}
              </span>
            )}
          </div>
        )}
      </div>
    </Card>
  );
};
