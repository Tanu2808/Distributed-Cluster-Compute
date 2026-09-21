import type { ClusterStatus } from "../../types";

interface ClusterStatusBadgeProps {
  status: ClusterStatus;
}

const config: Record<string, { label: string; dot: string; classes: string }> =
  {
    HEALTHY: {
      label: "Healthy",
      dot: "bg-green-400",
      classes: "bg-green-500/10 text-green-400 border-green-500/20",
    },
    ONLINE: {
      label: "Healthy",
      dot: "bg-green-400",
      classes: "bg-green-500/10 text-green-400 border-green-500/20",
    },
    DEGRADED: {
      label: "Degraded",
      dot: "bg-amber-400",
      classes: "bg-amber-500/10 text-amber-400 border-amber-500/20",
    },
    CRITICAL: {
      label: "Critical",
      dot: "bg-red-400",
      classes: "bg-red-500/10 text-red-400 border-red-500/20",
    },
    OFFLINE: {
      label: "Offline",
      dot: "bg-slate-500",
      classes: "bg-slate-700/30 text-slate-400 border-slate-600/20",
    },
  };

export function ClusterStatusBadge({ status }: ClusterStatusBadgeProps) {
  const { label, dot, classes } = config[status] || config.OFFLINE;
  return (
    <span
      className={`inline-flex items-center gap-2 px-3 py-1 rounded-full border text-sm font-semibold ${classes}`}
    >
      <span className={`w-2 h-2 rounded-full animate-pulse ${dot}`} />
      {label}
    </span>
  );
}
