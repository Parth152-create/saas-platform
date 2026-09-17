import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';
import { Button } from './Button';

export interface ErrorAlertProps {
  title?: string;
  message: string;
  details?: string[];
  onRetry?: () => void;
  className?: string;
}

export const ErrorAlert: React.FC<ErrorAlertProps> = ({
  title = 'An error occurred',
  message,
  details,
  onRetry,
  className = '',
}) => {
  return (
    <div
      className={`rounded-xl border border-red-200 bg-red-50 p-4 text-red-900 dark:border-red-900/60 dark:bg-red-950/40 dark:text-red-200 ${className}`}
      role="alert"
    >
      <div className="flex items-start gap-3">
        <AlertTriangle className="w-5 h-5 text-red-600 dark:text-red-400 shrink-0 mt-0.5" />
        <div className="flex-1 min-w-0">
          <h4 className="text-sm font-semibold">{title}</h4>
          <p className="mt-1 text-xs text-red-800 dark:text-red-300 leading-relaxed">{message}</p>
          {details && details.length > 0 && (
            <ul className="mt-2 text-xs list-disc list-inside space-y-0.5 text-red-700 dark:text-red-400">
              {details.map((d, i) => (
                <li key={i}>{d}</li>
              ))}
            </ul>
          )}
          {onRetry && (
            <div className="mt-3">
              <Button
                variant="outline"
                size="sm"
                onClick={onRetry}
                leftIcon={<RefreshCw className="w-3.5 h-3.5" />}
                className="bg-white/80 dark:bg-[#141414] border-red-300 dark:border-red-800 text-red-700 dark:text-red-300 hover:bg-white"
              >
                Retry
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
