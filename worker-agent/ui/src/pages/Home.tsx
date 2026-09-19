import { useState } from "react";
import { Link } from "react-router-dom";
import { RefreshCw, Copy, Check, ArrowRight } from "lucide-react";
import { useDashboardHome } from "../hooks/useDashboardHome";
import { useWorkerStatus } from "../hooks/useWorkerStatus";
import { useConnectionStatus } from "../hooks/useConnectionStatus";
import { useDashboardNode } from "../hooks/useDashboardNode";
import {
  ConsoleCard,
  MetricCard,
  KeyValueTable,
  StatusBadge,
  AlertBanner,
  Skeleton,
} from "../components";

function formatMb(mb: number | undefined): string {
  if (mb === undefined || mb === null || isNaN(mb) || mb < 0)
    return "Unavailable";
  if (mb >= 1024) {
    return `${(mb / 1024).toFixed(1)} GB`;
  }
  return `${Math.round(mb)} MB`;
}

function formatHeartbeat(isoString: string | null | undefined): string {
  if (!isoString) return "None";
  try {
    const date = new Date(isoString);
    if (isNaN(date.getTime())) return "None";
    return date.toLocaleTimeString();
  } catch {
    return "None";
  }
}

export default function Home() {
  const {
    data: homeData,
    loading,
    refreshing,
    error,
    refetch,
  } = useDashboardHome(4000);
  const { status, info } = useWorkerStatus(4000);
  const { connection } = useConnectionStatus(4000);
  const { data: nodeData } = useDashboardNode(10000);

  const [copiedId, setCopiedId] = useState(false);

  const handleCopyId = () => {
    if (!info?.workerId) return;
    navigator.clipboard.writeText(info.workerId);
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 1800);
  };

  // Safe Metric Calculations
  const hasCpu =
    typeof homeData?.cpuUsagePercent === "number" &&
    !isNaN(homeData.cpuUsagePercent) &&
    homeData.cpuUsagePercent >= 0;
  const cpuValue = hasCpu
    ? `${homeData!.cpuUsagePercent.toFixed(1)}%`
    : "Unavailable";
  const cpuSubtext = homeData?.cpuCores
    ? `${homeData.cpuCores} cores`
    : undefined;
  const cpuProgress = hasCpu ? homeData!.cpuUsagePercent : undefined;

  const hasMem =
    typeof homeData?.ramTotalMb === "number" &&
    homeData.ramTotalMb > 0 &&
    typeof homeData?.ramUsageMb === "number" &&
    homeData.ramUsageMb >= 0;
  const memValue = hasMem
    ? `${((homeData!.ramUsageMb / homeData!.ramTotalMb) * 100).toFixed(1)}%`
    : "Unavailable";
  const memSubtext = hasMem
    ? `${formatMb(homeData!.ramUsageMb)} / ${formatMb(homeData!.ramTotalMb)}`
    : undefined;
  const memProgress = hasMem
    ? (homeData!.ramUsageMb / homeData!.ramTotalMb) * 100
    : undefined;

  const hasStorage =
    typeof homeData?.storageTotalMb === "number" &&
    homeData.storageTotalMb > 0 &&
    typeof homeData?.storageUsageMb === "number" &&
    homeData.storageUsageMb >= 0;
  const storageValue = hasStorage
    ? `${((homeData!.storageUsageMb / homeData!.storageTotalMb) * 100).toFixed(1)}%`
    : "Unavailable";
  const storageSubtext = hasStorage
    ? `${formatMb(homeData!.storageUsageMb)} / ${formatMb(homeData!.storageTotalMb)}`
    : undefined;
  const storageProgress = hasStorage
    ? (homeData!.storageUsageMb / homeData!.storageTotalMb) * 100
    : undefined;

  const currentWorkerStatus =
    status?.lifecycleState || homeData?.workerStatus || "UNKNOWN";
  const currentCoordinatorStatus =
    connection?.connectionState || homeData?.coordinatorConnection || "UNKNOWN";

  // Initial Loading Skeleton
  if (loading && !homeData) {
    return (
      <div className="space-y-4 max-w-7xl">
        <div className="flex items-center justify-between pb-2 border-b border-console-border">
          <Skeleton className="h-6 w-44" />
          <div className="flex items-center gap-2">
            <Skeleton className="h-6 w-24" />
            <Skeleton className="h-7 w-20" />
          </div>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
          <Skeleton className="h-24" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Skeleton className="h-56" />
          <Skeleton className="h-56" />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-4 max-w-7xl">
      {/* 1. Page Header */}
      <div className="flex items-center justify-between pb-2.5 border-b border-console-border">
        <div className="flex items-center gap-3">
          <h1 className="text-base font-semibold tracking-tight text-console-text">
            Worker Overview
          </h1>
          <StatusBadge status={currentWorkerStatus} />
        </div>

        <button
          type="button"
          onClick={() => refetch()}
          disabled={refreshing}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors disabled:opacity-50"
          title="Refresh metrics"
        >
          <RefreshCw
            className={`w-3.5 h-3.5 text-console-textDim ${refreshing ? "animate-spin" : ""}`}
          />
          <span>Refresh</span>
        </button>
      </div>

      {/* Error Alert with Retry */}
      {error && (
        <AlertBanner
          type="error"
          message={
            <div className="flex items-center justify-between gap-4">
              <span>{error}</span>
              <button
                onClick={() => refetch()}
                className="underline hover:text-console-accent text-rose-800 shrink-0 font-sans font-medium"
              >
                Retry
              </button>
            </div>
          }
        />
      )}

      {/* 2. Resource Utilization Tiles */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <MetricCard
          label="CPU Utilization"
          value={cpuValue}
          subtext={cpuSubtext}
          progressPercent={cpuProgress}
          status={
            cpuProgress && cpuProgress >= 90
              ? "critical"
              : cpuProgress && cpuProgress >= 75
                ? "warning"
                : "normal"
          }
        />
        <MetricCard
          label="Memory Utilization"
          value={memValue}
          subtext={memSubtext}
          progressPercent={memProgress}
          status={
            memProgress && memProgress >= 90
              ? "critical"
              : memProgress && memProgress >= 75
                ? "warning"
                : "normal"
          }
        />
        <MetricCard
          label="Storage Utilization"
          value={storageValue}
          subtext={storageSubtext}
          progressPercent={storageProgress}
          status={
            storageProgress && storageProgress >= 90
              ? "critical"
              : storageProgress && storageProgress >= 75
                ? "warning"
                : "normal"
          }
        />
      </div>

      {/* 3. Operational Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Column 1: Worker Summary & Coordinator Connection */}
        <div className="space-y-4">
          {/* Worker Summary */}
          <ConsoleCard title="Worker Summary">
            <KeyValueTable
              columns={1}
              items={[
                {
                  label: "Worker ID",
                  mono: true,
                  value: info?.workerId ? (
                    <button
                      type="button"
                      onClick={handleCopyId}
                      className="inline-flex items-center gap-1.5 hover:text-console-accent text-console-text transition-colors"
                      title="Click to copy"
                    >
                      <span className="truncate max-w-[220px] md:max-w-[320px]">
                        {info.workerId}
                      </span>
                      {copiedId ? (
                        <Check className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                      ) : (
                        <Copy className="w-3.5 h-3.5 text-console-textDim hover:text-console-text shrink-0" />
                      )}
                    </button>
                  ) : (
                    "Unavailable"
                  ),
                },
                {
                  label: "Hostname",
                  value: homeData?.workerHostname || "Unavailable",
                },
                {
                  label: "Cluster",
                  value: homeData?.clusterName || "Not configured",
                },
                {
                  label: "Lifecycle State",
                  value: (
                    <StatusBadge
                      status={status?.lifecycleState || homeData?.workerStatus}
                    />
                  ),
                },
                {
                  label: "Execution State",
                  value: (
                    <StatusBadge status={status?.executionState || "IDLE"} />
                  ),
                },
              ]}
            />
          </ConsoleCard>

          {/* Coordinator Connection */}
          <ConsoleCard
            title="Coordinator Connection"
            actions={
              <Link
                to="/connection"
                className="text-[11px] text-console-accent hover:text-console-accentHover font-medium inline-flex items-center gap-1 transition-colors"
              >
                <span>Diagnostics</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            }
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between py-1 border-b border-console-borderSubtle text-xs">
                <span className="text-console-textDim font-medium">
                  Endpoint
                </span>
                <span
                  className="font-mono text-[11px] text-console-text truncate max-w-[240px] md:max-w-[320px]"
                  title={connection?.coordinatorUrl || undefined}
                >
                  {connection?.coordinatorUrl || "Not configured"}
                </span>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-console-borderSubtle text-xs">
                <span className="text-console-textDim font-medium">
                  Connection State
                </span>
                <StatusBadge status={currentCoordinatorStatus} />
              </div>
              <div className="flex items-center justify-between py-1 border-b border-console-borderSubtle text-xs">
                <span className="text-console-textDim font-medium">
                  Last Heartbeat
                </span>
                <span className="font-mono text-[11px] text-console-text">
                  {formatHeartbeat(connection?.lastSuccessfulHeartbeat)}
                </span>
              </div>
            </div>
          </ConsoleCard>
        </div>

        {/* Column 2: Task Execution & Node Hardware Summary */}
        <div className="space-y-4">
          {/* Task Execution */}
          <ConsoleCard
            title="Task Execution"
            actions={
              <Link
                to="/tasks"
                className="text-[11px] text-console-accent hover:text-console-accentHover font-medium inline-flex items-center gap-1 transition-colors"
              >
                <span>View tasks</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            }
          >
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-3 bg-console-subtle border border-console-border rounded-sm">
                  <span className="text-[10px] font-medium uppercase tracking-wider text-console-textDim block">
                    Active Tasks
                  </span>
                  <span className="text-2xl font-mono font-semibold text-console-text mt-1 block">
                    {homeData?.activeTasks ?? 0}
                  </span>
                </div>
                <div className="p-3 bg-console-subtle border border-console-border rounded-sm">
                  <span className="text-[10px] font-medium uppercase tracking-wider text-console-textDim block">
                    Queued Tasks
                  </span>
                  <span className="text-2xl font-mono font-semibold text-console-textDim mt-1 block">
                    {homeData?.queuedTasks ?? 0}
                  </span>
                </div>
              </div>

              <div className="flex items-center justify-between py-1 border-t border-console-borderSubtle text-xs pt-3">
                <span className="text-console-textDim font-medium">
                  Execution Status
                </span>
                <StatusBadge status={status?.executionState || "IDLE"} />
              </div>
            </div>
          </ConsoleCard>

          {/* Node Hardware Summary */}
          <ConsoleCard
            title="Node Hardware Summary"
            actions={
              <Link
                to="/node"
                className="text-[11px] text-console-accent hover:text-console-accentHover font-medium inline-flex items-center gap-1 transition-colors"
              >
                <span>View node details</span>
                <ArrowRight className="w-3 h-3" />
              </Link>
            }
          >
            <KeyValueTable
              columns={1}
              items={[
                {
                  label: "CPU Model",
                  value: nodeData?.cpuModel || "Unavailable",
                },
                {
                  label: "Cores",
                  value: homeData?.cpuCores
                    ? `${homeData.cpuCores} cores`
                    : "Unavailable",
                },
                {
                  label: "Total Memory",
                  value: formatMb(homeData?.ramTotalMb),
                },
                {
                  label: "Platform",
                  value: nodeData
                    ? `${nodeData.osName} (${nodeData.architecture})`
                    : "Unavailable",
                },
                {
                  label: "GPU Units",
                  value:
                    typeof homeData?.gpuCount === "number"
                      ? `${homeData.gpuCount}`
                      : "Unavailable",
                },
              ]}
            />
          </ConsoleCard>
        </div>
      </div>
    </div>
  );
}
