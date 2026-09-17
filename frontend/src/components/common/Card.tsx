import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
  className?: string;
  hoverEffect?: boolean;
}

export const Card: React.FC<CardProps> = ({
  children,
  className = '',
  hoverEffect = false,
  ...props
}) => {
  return (
    <div
      className={`rounded-xl border border-neutral-200 bg-white text-neutral-900 shadow-xs dark:border-[#262626] dark:bg-[#141414] dark:text-neutral-100 ${
        hoverEffect ? 'hover:border-neutral-300 dark:hover:border-[#383838] hover:shadow-md transition-all' : ''
      } ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};

export const CardHeader: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className = '',
  ...props
}) => (
  <div
    className={`flex flex-col space-y-1.5 p-5 pb-4 border-b border-neutral-100 dark:border-[#262626] ${className}`}
    {...props}
  />
);

export const CardTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({
  className = '',
  ...props
}) => (
  <h3
    className={`text-base font-semibold leading-none tracking-tight text-neutral-900 dark:text-neutral-100 ${className}`}
    {...props}
  />
);

export const CardDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({
  className = '',
  ...props
}) => (
  <p className={`text-xs text-neutral-500 dark:text-neutral-400 mt-1 ${className}`} {...props} />
);

export const CardContent: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className = '',
  ...props
}) => <div className={`p-5 ${className}`} {...props} />;

export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className = '',
  ...props
}) => (
  <div
    className={`flex items-center p-4 border-t border-neutral-100 dark:border-[#262626] ${className}`}
    {...props}
  />
);
