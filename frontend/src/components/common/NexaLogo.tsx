import React from 'react';

export interface NexaMarkProps {
  size?: number | string;
  className?: string;
  theme?: 'auto' | 'dark' | 'light';
  withContainer?: boolean;
  containerClassName?: string;
}

export const NexaMark: React.FC<NexaMarkProps> = ({
  size = 32,
  className = '',
  theme = 'auto',
  withContainer = false,
  containerClassName = '',
}) => {
  const stemColor =
    theme === 'dark'
      ? 'text-white fill-white'
      : theme === 'light'
        ? 'text-[#0a0a0a] fill-[#0a0a0a]'
        : 'text-neutral-900 dark:text-white fill-current';

  const svgElement = (
    <svg
      width={size}
      height={size}
      viewBox="0 0 32 32"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={`shrink-0 transition-colors select-none ${className}`}
      aria-label="Nexa Logo Mark"
    >
      {/* Left Pillar / Foundation Structure */}
      <path
        d="M5 12.5A1.5 1.5 0 0 1 6.5 11H8.25L17.1 28H6.5A1.5 1.5 0 0 1 5 26.5V12.5Z"
        className={stemColor}
      />
      {/* Electric Blue Diagonal Workflow Connector */}
      <path
        d="M8.2 4H12.5L23.8 28H19.5L8.2 4Z"
        fill="#2563EB"
      />
      {/* Right Pillar / Operations Structure */}
      <path
        d="M27 19.5A1.5 1.5 0 0 1 25.5 21H23.75L14.9 4H25.5A1.5 1.5 0 0 1 27 5.5V19.5Z"
        className={stemColor}
      />
    </svg>
  );

  if (withContainer) {
    const defaultContainerClass =
      theme === 'dark'
        ? 'bg-[#141414] border border-[#262626]'
        : theme === 'light'
          ? 'bg-neutral-100 border border-neutral-200'
          : 'bg-neutral-100 dark:bg-[#141414] border border-neutral-200/80 dark:border-[#262626]';

    return (
      <div
        className={`inline-flex items-center justify-center rounded-xl p-2 shrink-0 ${defaultContainerClass} ${containerClassName}`}
      >
        {svgElement}
      </div>
    );
  }

  return svgElement;
};

export interface NexaLogoProps {
  variant?: 'full' | 'compact' | 'mark';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  theme?: 'auto' | 'dark' | 'light';
  className?: string;
  withContainer?: boolean;
  descriptor?: string;
}

export const NexaLogo: React.FC<NexaLogoProps> = ({
  variant = 'full',
  size = 'md',
  theme = 'auto',
  className = '',
  withContainer = false,
  descriptor = 'WORKFORCE & OPERATIONS',
}) => {
  const markSizeMap = {
    sm: 22,
    md: 28,
    lg: 36,
    xl: 44,
  };

  const wordmarkSizeMap = {
    sm: 'text-sm font-bold tracking-tight',
    md: 'text-base font-bold tracking-tight leading-tight',
    lg: 'text-xl font-bold tracking-tight leading-tight',
    xl: 'text-2xl font-extrabold tracking-tight leading-tight',
  };

  const descriptorSizeMap = {
    sm: 'text-[8.5px] font-semibold tracking-[0.14em]',
    md: 'text-[9.5px] font-semibold tracking-[0.16em]',
    lg: 'text-[11px] font-semibold tracking-[0.16em]',
    xl: 'text-xs font-semibold tracking-[0.18em]',
  };

  const wordmarkColor =
    theme === 'dark'
      ? 'text-white'
      : theme === 'light'
        ? 'text-neutral-900'
        : 'text-neutral-900 dark:text-neutral-100';

  const descriptorColor =
    theme === 'dark'
      ? 'text-neutral-400'
      : theme === 'light'
        ? 'text-neutral-500'
        : 'text-neutral-500 dark:text-neutral-400';

  if (variant === 'mark') {
    return (
      <div className={`inline-flex items-center ${className}`}>
        <NexaMark
          size={markSizeMap[size]}
          theme={theme}
          withContainer={withContainer}
        />
      </div>
    );
  }

  return (
    <div className={`inline-flex items-center gap-3 select-none ${className}`}>
      <NexaMark
        size={markSizeMap[size]}
        theme={theme}
        withContainer={withContainer}
      />
      <div className="flex flex-col min-w-0 justify-center">
        <span className={`${wordmarkSizeMap[size]} ${wordmarkColor} font-sans`}>
          Nexa
        </span>
        {variant === 'full' && (
          <span
            className={`${descriptorSizeMap[size]} ${descriptorColor} uppercase font-sans mt-0.5 leading-none`}
          >
            {descriptor}
          </span>
        )}
      </div>
    </div>
  );
};
export default NexaLogo;
