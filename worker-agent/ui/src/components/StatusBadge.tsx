import React from "react";

interface StatusBadgeProps {
  status?: string | null;
  label?: string;
  size?: "sm" | "md";
  className?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  label,
  size = "sm",
  className = "",
}) => {
  const s = (status || "UNKNOWN").toUpperCase();
  const displayLabel = label || s;

  let colorClasses = "bg-slate-100 text-slate-700 border-slate-300";
  let dotClasses = "bg-slate-500";

  if (["ONLINE", "SUCCESS", "CONFIGURED", "COMPLETED", "RUNNING"].includes(s)) {
    colorClasses = "bg-emerald-50 text-emerald-800 border-emerald-300";
    dotClasses = "bg-emerald-600";
  } else if (
    [
      "BUSY",
      "CONNECTING",
      "REGISTERING",
      "RECONNECTING",
      "SETUP_REQUIRED",
      "VALIDATING",
      "INITIALIZING",
    ].includes(s)
  ) {
    colorClasses = "bg-amber-50 text-amber-900 border-amber-300";
    dotClasses = "bg-amber-600 animate-pulse";
  } else if (["IDLE", "QUEUED"].includes(s)) {
    colorClasses = "bg-sky-50 text-sky-800 border-sky-300";
    dotClasses = "bg-sky-600";
  } else if (
    ["DISCONNECTED", "OFFLINE", "FAILED", "CANCELLED", "REJECTED"].includes(s)
  ) {
    colorClasses = "bg-rose-50 text-rose-800 border-rose-300";
    dotClasses = "bg-rose-600";
  }

  const sizeClasses =
    size === "sm"
      ? "text-[11px] px-2 py-0.5 gap-1.5"
      : "text-xs px-2.5 py-1 gap-2";

  return (
    <span
      className={`inline-flex items-center font-mono font-medium rounded-sm border ${colorClasses} ${sizeClasses} ${className}`}
      title={s}
    >
      <span className={`w-1.5 h-1.5 rounded-full shrink-0 ${dotClasses}`} />
      <span className="truncate">{displayLabel}</span>
    </span>
  );
};
