import React, { useState } from 'react';
import { useTheme } from '../../context/ThemeContext';

export interface ChartDataPoint {
  label: string;
  value: number;
  secondaryValue?: number;
}

export interface BarChartProps {
  data: ChartDataPoint[];
  height?: number;
  barColor?: string;
  darkBarColor?: string;
  valueFormatter?: (v: number) => string;
}

export const SimpleBarChart: React.FC<BarChartProps> = ({
  data,
  height = 180,
  barColor = '#18181b',
  darkBarColor,
  valueFormatter = (v) => v.toString(),
}) => {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === 'dark';
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  // High contrast in dark mode against dark card background while keeping exact light mode color
  const activeBarColor = isDark
    ? darkBarColor || (barColor === '#18181b' ? '#e4e4e7' : barColor)
    : barColor;

  const maxValue = Math.max(...data.map((d) => d.value), 10);

  return (
    <div
      className="w-full select-none"
      onMouseLeave={() => setHoveredIndex(null)}
    >
      {/* Chart container with headroom for tooltip */}
      <div className="relative pt-10 pb-1">
        {/* Subtle background reference lines for dark and light mode readability */}
        <div className="absolute inset-x-2 inset-y-0 flex flex-col justify-between pointer-events-none opacity-40 dark:opacity-20">
          <div className="border-b border-dashed border-neutral-200 dark:border-[#262626] w-full" />
          <div className="border-b border-dashed border-neutral-200 dark:border-[#262626] w-full" />
          <div className="border-b border-dashed border-neutral-200 dark:border-[#262626] w-full" />
        </div>

        {/* Bars row */}
        <div
          className="relative flex items-end justify-between gap-2 sm:gap-4 px-2"
          style={{ height: `${height}px` }}
        >
          {data.map((point, i) => {
            const heightPercent = Math.max(8, Math.round((point.value / maxValue) * 100));
            const isHovered = hoveredIndex === i;
            const isAnyHovered = hoveredIndex !== null;

            return (
              <div
                key={point.label}
                className="relative flex-1 flex flex-col items-center h-full justify-end group cursor-pointer"
                onMouseEnter={() => setHoveredIndex(i)}
              >
                {/* Tooltip following application theme */}
                {isHovered && (
                  <div
                    className={`absolute -top-9 z-30 pointer-events-none px-2.5 py-1 rounded-md border border-neutral-200 bg-white text-neutral-900 shadow-md dark:border-[#262626] dark:bg-[#1a1a1a] dark:text-white text-xs font-semibold whitespace-nowrap animate-in fade-in zoom-in-95 duration-150 flex items-center gap-1.5 ${
                      i === 0
                        ? 'left-0 sm:left-1/2 sm:-translate-x-1/2'
                        : i === data.length - 1
                        ? 'right-0 sm:left-1/2 sm:-translate-x-1/2'
                        : 'left-1/2 -translate-x-1/2'
                    }`}
                  >
                    <span className="text-[11px] font-medium text-neutral-500 dark:text-neutral-400">
                      {point.label}:
                    </span>
                    <span>{valueFormatter(point.value)}</span>
                  </div>
                )}

                {/* Bar */}
                <div
                  className="w-full max-w-8 sm:max-w-10 rounded-t-md transition-all duration-200 ease-out"
                  style={{
                    height: `${heightPercent}%`,
                    backgroundColor: isHovered
                      ? isDark
                        ? '#ffffff'
                        : '#09090b'
                      : activeBarColor,
                    opacity: isHovered ? 1 : isAnyHovered ? 0.35 : 1,
                    transform: isHovered ? 'scaleY(1.03)' : 'scaleY(1)',
                    transformOrigin: 'bottom',
                    filter: isHovered
                      ? isDark
                        ? 'drop-shadow(0 0 6px rgba(255,255,255,0.45))'
                        : 'drop-shadow(0 3px 6px rgba(0,0,0,0.25))'
                      : undefined,
                  }}
                />
              </div>
            );
          })}
        </div>
      </div>

      {/* X-axis labels & border */}
      <div className="flex justify-between gap-2 sm:gap-4 border-t border-neutral-200 dark:border-[#262626] pt-2.5 px-2 mt-1">
        {data.map((point, i) => (
          <span
            key={point.label}
            className={`flex-1 text-center text-[11px] font-medium transition-colors truncate ${
              hoveredIndex === i
                ? 'text-neutral-900 dark:text-neutral-100 font-semibold'
                : 'text-neutral-500 dark:text-neutral-400'
            }`}
          >
            {point.label}
          </span>
        ))}
      </div>
    </div>
  );
};

export interface DonutSegment {
  label: string;
  value: number;
  color: string;
  darkColor?: string;
}

export interface DonutChartProps {
  segments: DonutSegment[];
  size?: number;
  centerText?: string;
  centerSubtext?: string;
}

export const SimpleDonutChart: React.FC<DonutChartProps> = ({
  segments,
  size = 170,
  centerText,
  centerSubtext,
}) => {
  const { resolvedTheme } = useTheme();
  const isDark = resolvedTheme === 'dark';
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const total = segments.reduce((sum, s) => sum + s.value, 0) || 1;
  const normalStrokeWidth = 22;
  const normalRadius = (size - 36) / 2;

  // Render hovered segment last so it paints cleanly on top of neighboring segment caps
  const segmentIndices = segments.map((_, i) => i);
  const orderedIndices =
    hoveredIndex !== null
      ? [...segmentIndices.filter((idx) => idx !== hoveredIndex), hoveredIndex]
      : segmentIndices;

  const isAnyHovered = hoveredIndex !== null;
  const activeHoveredSegment = hoveredIndex !== null ? segments[hoveredIndex] : null;

  return (
    <div
      className="relative flex flex-col 2xl:flex-row items-center justify-around gap-4 sm:gap-6 pt-9 pb-2 w-full select-none"
      onMouseLeave={() => setHoveredIndex(null)}
    >
      {/* Floating Theme-Aware Tooltip inside chart area with safe clearance */}
      {activeHoveredSegment && (
        <div className="absolute top-1 left-1/2 -translate-x-1/2 z-30 pointer-events-none px-3 py-1.5 rounded-lg border border-neutral-200 bg-white text-neutral-900 shadow-md dark:border-[#262626] dark:bg-[#1a1a1a] dark:text-white text-xs font-semibold whitespace-nowrap animate-in fade-in zoom-in-95 duration-150 flex items-center gap-2">
          <span
            className="w-2.5 h-2.5 rounded-full shrink-0"
            style={{
              backgroundColor: isDark
                ? activeHoveredSegment.darkColor || activeHoveredSegment.color
                : activeHoveredSegment.color,
            }}
          />
          <span className="font-medium text-neutral-600 dark:text-neutral-300">
            {activeHoveredSegment.label}:
          </span>
          <span className="font-bold text-neutral-900 dark:text-white">
            {activeHoveredSegment.value}
          </span>
          <span className="text-[11px] text-neutral-400 font-normal">
            ({Math.round((activeHoveredSegment.value / total) * 100)}%)
          </span>
        </div>
      )}

      {/* SVG Donut Circle */}
      <div className="relative shrink-0" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="transform -rotate-90 overflow-visible">
          {/* Background subtle track */}
          <circle
            cx={size / 2}
            cy={size / 2}
            r={normalRadius}
            fill="transparent"
            stroke="currentColor"
            className="text-neutral-100 dark:text-[#222222]"
            strokeWidth={normalStrokeWidth}
          />

          {/* Segment arcs */}
          {orderedIndices.map((i) => {
            const seg = segments[i];
            const isHovered = hoveredIndex === i;
            const segColor = isDark ? seg.darkColor || seg.color : seg.color;
            const segRadius = isHovered ? normalRadius + 2.5 : normalRadius;
            const segCircumference = 2 * Math.PI * segRadius;
            const percent = (seg.value / total) * 100;
            const strokeDasharray = `${(percent * segCircumference) / 100} ${segCircumference}`;
            const prevPercent = segments
              .slice(0, i)
              .reduce((sum, s) => sum + (s.value / total) * 100, 0);
            const strokeDashoffset = -((prevPercent * segCircumference) / 100);

            return (
              <circle
                key={i}
                cx={size / 2}
                cy={size / 2}
                r={segRadius}
                fill="transparent"
                stroke={segColor}
                strokeWidth={isHovered ? 28 : normalStrokeWidth}
                strokeDasharray={strokeDasharray}
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                className="cursor-pointer transition-all duration-200 ease-out"
                style={{
                  opacity: isHovered ? 1 : isAnyHovered ? 0.35 : 1,
                  filter: isHovered
                    ? isDark
                      ? 'drop-shadow(0 0 8px rgba(255,255,255,0.4))'
                      : 'drop-shadow(0 3px 8px rgba(0,0,0,0.25))'
                    : undefined,
                  pointerEvents: 'stroke',
                }}
                onMouseEnter={() => setHoveredIndex(i)}
              />
            );
          })}
        </svg>

        {/* Stable Center Label - Never moves, jumps or offsets */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center pointer-events-none select-none">
          {centerText && (
            <span className="text-xl font-bold text-neutral-900 dark:text-neutral-100 leading-tight">
              {centerText}
            </span>
          )}
          {centerSubtext && (
            <span className="text-[10px] text-neutral-400 uppercase tracking-wider font-semibold mt-0.5">
              {centerSubtext}
            </span>
          )}
        </div>
      </div>

      {/* Legend with interactive hover sync */}
      <div className="flex flex-col gap-2 min-w-32">
        {segments.map((s, idx) => {
          const isHovered = hoveredIndex === idx;
          const segColor = isDark ? s.darkColor || s.color : s.color;

          return (
            <div
              key={idx}
              className={`flex items-center justify-between gap-3 text-xs cursor-pointer rounded-md px-2 py-1 transition-all ${
                isHovered
                  ? 'bg-neutral-100 dark:bg-[#1f1f1f] shadow-xs'
                  : isAnyHovered
                  ? 'opacity-40'
                  : 'hover:bg-neutral-50 dark:hover:bg-[#1a1a1a]'
              }`}
              onMouseEnter={() => setHoveredIndex(idx)}
              onMouseLeave={() => setHoveredIndex(null)}
            >
              <div className="flex items-center gap-2 min-w-0">
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0 transition-transform"
                  style={{
                    backgroundColor: segColor,
                    transform: isHovered ? 'scale(1.2)' : 'scale(1)',
                  }}
                />
                <span className="text-neutral-600 dark:text-neutral-300 truncate font-medium">
                  {s.label}
                </span>
              </div>
              <span className="font-semibold text-neutral-900 dark:text-neutral-100 shrink-0">
                {s.value}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
};
