import type { ReactNode } from 'react';

interface ResourceCardProps {
  label: string;
  icon: ReactNode;
  used: number;
  total: number;
  usedLabel: string;
  totalLabel: string;
  unit?: string;
  colorClass?: string;
  className?: string;
}

function getUtilizationColor(pct: number): string {
  if (pct >= 90) return 'bg-red-500';
  if (pct >= 75) return 'bg-amber-500';
  if (pct >= 50) return 'bg-cyan-500';
  return 'bg-green-500';
}

export function ResourceCard({
  label, icon, used, total, usedLabel, totalLabel, colorClass, className = ''
}: ResourceCardProps) {
  const pct = total > 0 ? Math.round((used / total) * 100) : 0;
  const fillColor = colorClass ?? getUtilizationColor(pct);

  return (
    <div className={`card-hover p-5 ${className}`}>
      <div className="flex items-center gap-2.5 mb-4">
        <span className="text-slate-400">{icon}</span>
        <span className="section-title">{label}</span>
      </div>

      {/* Utilization ring */}
      <div className="flex items-center gap-5">
        {/* Ring */}
        <div className="relative w-20 h-20 flex-shrink-0">
          <svg className="w-20 h-20 -rotate-90" viewBox="0 0 80 80">
            {/* Track */}
            <circle cx="40" cy="40" r="32" fill="none" stroke="rgba(255,255,255,0.05)" strokeWidth="8" />
            {/* Fill */}
            <circle
              cx="40" cy="40" r="32" fill="none"
              strokeWidth="8"
              stroke={
                pct >= 90 ? '#ef4444' : pct >= 75 ? '#f59e0b' : pct >= 50 ? '#06b6d4' : '#4ade80'
              }
              strokeDasharray={`${2 * Math.PI * 32}`}
              strokeDashoffset={`${2 * Math.PI * 32 * (1 - pct / 100)}`}
              strokeLinecap="round"
              style={{ transition: 'stroke-dashoffset 0.8s ease' }}
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <span className="text-sm font-bold text-slate-100">{pct}%</span>
          </div>
        </div>

        {/* Details */}
        <div className="flex-1">
          <div className="flex justify-between items-baseline mb-1">
            <span className="text-xs text-slate-500">Used</span>
            <span className="text-sm font-semibold text-slate-200">{usedLabel}</span>
          </div>
          <div className="progress-bar mb-3">
            <div
              className={`progress-fill ${fillColor}`}
              style={{ width: `${pct}%` }}
            />
          </div>
          <div className="flex justify-between items-baseline">
            <span className="text-xs text-slate-500">Total</span>
            <span className="text-sm text-slate-400">{totalLabel}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
