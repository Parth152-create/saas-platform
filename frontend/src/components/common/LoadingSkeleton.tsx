import React from 'react';

export const Skeleton: React.FC<{ className?: string }> = ({ className = '' }) => (
  <div
    className={`animate-pulse rounded-lg bg-neutral-200 dark:bg-[#1f1f1f] ${className}`}
  />
);

export const SkeletonCard: React.FC = () => (
  <div className="rounded-xl border border-neutral-200 bg-white p-5 dark:border-[#262626] dark:bg-[#141414] space-y-3">
    <div className="flex items-center justify-between">
      <Skeleton className="h-4 w-28" />
      <Skeleton className="h-8 w-8 rounded-full" />
    </div>
    <Skeleton className="h-7 w-20" />
    <Skeleton className="h-3 w-36" />
  </div>
);

export const SkeletonTable: React.FC<{ rows?: number }> = ({ rows = 5 }) => (
  <div className="rounded-xl border border-neutral-200 bg-white dark:border-[#262626] dark:bg-[#141414] overflow-hidden">
    <div className="p-4 border-b border-neutral-100 dark:border-[#262626] flex justify-between items-center">
      <Skeleton className="h-5 w-40" />
      <Skeleton className="h-8 w-24" />
    </div>
    <div className="p-4 space-y-4">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex items-center gap-4">
          <Skeleton className="h-10 w-10 rounded-full shrink-0" />
          <div className="space-y-2 flex-1">
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-3 w-1/4" />
          </div>
          <Skeleton className="h-6 w-16 rounded-full shrink-0" />
          <Skeleton className="h-4 w-24 shrink-0" />
        </div>
      ))}
    </div>
  </div>
);

export interface LoadingSkeletonProps {
  variant?: 'card' | 'table' | 'text';
  rows?: number;
  className?: string;
}

export const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({ variant = 'card', rows = 5, className = '' }) => {
  if (variant === 'table') {
    return <SkeletonTable rows={rows} />;
  }
  if (variant === 'card') {
    return <SkeletonCard />;
  }
  return <Skeleton className={className} />;
};

