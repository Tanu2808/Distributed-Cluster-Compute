import type { ReactNode } from "react";

interface StatCardProps {
  label: string;
  value: string | number;
  subValue?: string;
  icon: ReactNode;
  accent?: "cyan" | "violet" | "green" | "amber" | "red";
  trend?: { value: number; label: string };
  className?: string;
}

const accentMap = {
  cyan: {
    iconBg: "bg-cyan-500/10",
    iconColor: "text-cyan-400",
    border: "hover:border-cyan-500/30",
  },
  violet: {
    iconBg: "bg-violet-500/10",
    iconColor: "text-violet-400",
    border: "hover:border-violet-500/30",
  },
  green: {
    iconBg: "bg-green-500/10",
    iconColor: "text-green-400",
    border: "hover:border-green-500/30",
  },
  amber: {
    iconBg: "bg-amber-500/10",
    iconColor: "text-amber-400",
    border: "hover:border-amber-500/30",
  },
  red: {
    iconBg: "bg-red-500/10",
    iconColor: "text-red-400",
    border: "hover:border-red-500/30",
  },
};

export function StatCard({
  label,
  value,
  subValue,
  icon,
  accent = "cyan",
  trend,
  className = "",
}: StatCardProps) {
  const { iconBg, iconColor, border } = accentMap[accent];

  return (
    <div className={`card-hover p-5 animate-fade-in ${border} ${className}`}>
      <div className="flex items-start justify-between">
        <div className="flex-1 min-w-0">
          <p className="section-title mb-2">{label}</p>
          <p className="text-2xl font-bold text-slate-100 leading-none">
            {value}
          </p>
          {subValue && (
            <p className="text-xs text-slate-500 mt-1.5">{subValue}</p>
          )}
          {trend && (
            <p
              className={`text-xs mt-2 font-medium ${trend.value >= 0 ? "text-green-400" : "text-red-400"}`}
            >
              {trend.value >= 0 ? "▲" : "▼"} {Math.abs(trend.value)}%{" "}
              {trend.label}
            </p>
          )}
        </div>
        <div
          className={`${iconBg} ${iconColor} p-2.5 rounded-lg ml-3 flex-shrink-0`}
        >
          {icon}
        </div>
      </div>
    </div>
  );
}
