import React from 'react';

interface MetricCardProps {
  label: string;
  value: string | number;
  subtext?: string;
  progressPercent?: number;
  status?: 'normal' | 'warning' | 'critical';
  className?: string;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  subtext,
  progressPercent,
  status = 'normal',
  className = '',
}) => {
  let progressColor = 'bg-console-accent';
  if (status === 'warning' || (progressPercent !== undefined && progressPercent >= 75 && progressPercent < 90)) {
    progressColor = 'bg-amber-500';
  } else if (status === 'critical' || (progressPercent !== undefined && progressPercent >= 90)) {
    progressColor = 'bg-rose-500';
  }

  return (
    <div className={`bg-console-surface border border-console-border rounded-sm p-3.5 flex flex-col justify-between ${className}`}>
      <span className="text-[11px] font-medium uppercase tracking-wider text-console-textDim">
        {label}
      </span>
      
      <div className="my-1.5 flex items-baseline justify-between gap-2">
        <span className="text-xl font-semibold font-mono tracking-tight text-console-text">
          {value}
        </span>
        {subtext && (
          <span className="text-xs text-console-textMuted truncate">
            {subtext}
          </span>
        )}
      </div>

      {progressPercent !== undefined && (
        <div className="mt-1">
          <div className="w-full bg-slate-200 h-1.5 rounded-sm overflow-hidden">
            <div
              className={`h-full transition-all duration-300 ${progressColor}`}
              style={{ width: `${Math.min(100, Math.max(0, progressPercent))}%` }}
            />
          </div>
        </div>
      )}
    </div>
  );
};
