import type { WorkerStatus } from "../../types";

interface StatusBadgeProps {
  status: WorkerStatus;
  size?: "sm" | "md";
}

const config: Record<WorkerStatus, { label: string; classes: string }> = {
  ONLINE: {
    label: "Online",
    classes: "bg-green-500/15 text-green-400 border-green-500/30",
  },
  BUSY: {
    label: "Busy",
    classes: "bg-amber-500/15 text-amber-400 border-amber-500/30",
  },
  OFFLINE: {
    label: "Offline",
    classes: "bg-slate-700/50 text-slate-400 border-slate-600/30",
  },
  UNHEALTHY: {
    label: "Unhealthy",
    classes: "bg-red-500/15 text-red-400 border-red-500/30",
  },
  REGISTERING: {
    label: "Registering",
    classes: "bg-blue-500/10 text-blue-400 border-blue-500/20",
  },
  HEARTBEAT_TIMEOUT: {
    label: "Timeout",
    classes: "bg-red-500/10 text-red-400 border-red-500/20",
  },
};

export function StatusBadge({ status, size = "md" }: StatusBadgeProps) {
  const { label, classes } = config[status];
  const sizeClass =
    size === "sm" ? "text-[10px] px-1.5 py-0.5" : "text-xs px-2.5 py-1";

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border font-medium ${sizeClass} ${classes}`}
    >
      <span
        className={`rounded-full ${status === "ONLINE" ? "animate-pulse" : ""} ${
          status === "BUSY" ? "animate-pulse" : ""
        } ${size === "sm" ? "w-1 h-1" : "w-1.5 h-1.5"} ${
          status === "ONLINE"
            ? "bg-green-400"
            : status === "BUSY"
              ? "bg-amber-400"
              : status === "OFFLINE"
                ? "bg-slate-500"
                : status === "UNHEALTHY"
                  ? "bg-red-400"
                  : "bg-blue-400"
        }`}
      />
      {label}
    </span>
  );
}
