import React from 'react';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost' | 'link';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  leftIcon,
  rightIcon,
  className = '',
  disabled,
  ...props
}) => {
  const baseStyles =
    'inline-flex items-center justify-center font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer select-none';

  const sizeStyles = {
    sm: 'text-xs px-2.5 py-1.5 gap-1.5',
    md: 'text-sm px-4 py-2 gap-2',
    lg: 'text-base px-5 py-2.5 gap-2.5',
  };

  const variantStyles = {
    primary:
      'bg-neutral-950 text-white hover:bg-neutral-800 focus:ring-neutral-900 shadow-xs border border-transparent dark:bg-white dark:text-[#0a0a0a] dark:hover:bg-neutral-100 dark:focus:ring-white',
    secondary:
      'bg-neutral-100 hover:bg-neutral-200 text-neutral-900 focus:ring-neutral-400 border border-neutral-200 dark:bg-[#1c1c1c] dark:hover:bg-[#262626] dark:text-neutral-100 dark:border-[#262626]',
    outline:
      'bg-transparent hover:bg-neutral-100 text-neutral-900 border border-neutral-300 focus:ring-neutral-900 dark:border-[#262626] dark:text-neutral-100 dark:hover:bg-[#1c1c1c] dark:focus:ring-white',
    danger:
      'bg-red-600 hover:bg-red-700 text-white focus:ring-red-500 shadow-xs border border-transparent dark:bg-red-600 dark:hover:bg-red-700',
    ghost:
      'bg-transparent hover:bg-neutral-100 text-neutral-700 hover:text-neutral-950 focus:ring-neutral-400 dark:text-neutral-300 dark:hover:bg-[#1c1c1c] dark:hover:text-white',
    link: 'bg-transparent text-neutral-900 hover:underline p-0 focus:ring-0 shadow-none dark:text-neutral-100 font-medium',
  };

  return (
    <button
      className={`${baseStyles} ${sizeStyles[size]} ${variantStyles[variant]} ${className}`}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <Loader2 className="w-4 h-4 animate-spin shrink-0" />
      ) : (
        leftIcon && <span className="shrink-0">{leftIcon}</span>
      )}
      <span>{children}</span>
      {!isLoading && rightIcon && <span className="shrink-0">{rightIcon}</span>}
    </button>
  );
};
