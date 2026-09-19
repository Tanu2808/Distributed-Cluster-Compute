import { useState } from "react";
import { RefreshCw, Download, Copy, Check } from "lucide-react";
import { useDashboardNode } from "../hooks/useDashboardNode";
import { useWorkerStatus } from "../hooks/useWorkerStatus";
import { useDashboardHome } from "../hooks/useDashboardHome";
import {
  ConsoleCard,
  KeyValueTable,
  StatusBadge,
  AlertBanner,
  Skeleton,
} from "../components";

function formatBytes(bytes?: number): string {
  if (bytes === undefined || bytes === null || isNaN(bytes) || bytes < 0)
    return "Unavailable";
  if (bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  if (i >= sizes.length)
    return `${(bytes / Math.pow(k, sizes.length - 1)).toFixed(2)} TB`;
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`;
}

function formatMb(mb?: number): string {
  if (mb === undefined || mb === null || isNaN(mb) || mb < 0)
    return "Unavailable";
  if (mb >= 1024) {
    return `${(mb / 1024).toFixed(1)} GB`;
  }
  return `${Math.round(mb)} MB`;
}

export default function Node() {
  const {
    data: nodeData,
    loading,
    refreshing,
    error,
    refetch,
  } = useDashboardNode(5000);
  const { status, info } = useWorkerStatus(5000);
  const { data: homeData } = useDashboardHome(5000);

  const [copiedId, setCopiedId] = useState(false);

  const handleCopyId = () => {
    if (!info?.workerId) return;
    navigator.clipboard.writeText(info.workerId);
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 1800);
  };

  const handleExportDiagnostics = () => {
    const diagnostics = {
      exportedAt: new Date().toISOString(),
      workerIdentity: {
        workerId: info?.workerId || "unavailable",
        hostname: homeData?.workerHostname || "unavailable",
        status: status?.lifecycleState || "unavailable",
        executionState: status?.executionState || "unavailable",
      },
      system: {
        osName: nodeData?.osName || "unavailable",
        osVersion: nodeData?.osVersion || "unavailable",
        architecture: nodeData?.architecture || "unavailable",
        agentVersion: nodeData?.agentVersion || "unavailable",
      },
      processor: {
        model: nodeData?.cpuModel || "unavailable",
        cores: homeData?.cpuCores ?? "unavailable",
        utilizationPercent: nodeData?.cpuUsagePercent ?? "unavailable",
      },
      memory: {
        usedMb: nodeData?.memoryUsedMb ?? "unavailable",
        totalMb: nodeData?.memoryTotalMb ?? "unavailable",
      },
      storage: {
        usedMb: nodeData?.diskUsedMb ?? "unavailable",
        totalMb: nodeData?.diskTotalMb ?? "unavailable",
      },
      network: {
        bytesSent: nodeData?.networkBytesSent ?? "unavailable",
        bytesReceived: nodeData?.networkBytesReceived ?? "unavailable",
      },
      gpu: {
        detected:
          typeof nodeData?.gpuCount === "number" && nodeData.gpuCount > 0,
        count: nodeData?.gpuCount ?? "unavailable",
      },
    };

    const blob = new Blob([JSON.stringify(diagnostics, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `worker-node-diagnostics-${info?.workerId || "node"}-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Safe Metric Computations
  const cpuValid =
    typeof nodeData?.cpuUsagePercent === "number" &&
    !isNaN(nodeData.cpuUsagePercent) &&
    nodeData.cpuUsagePercent >= 0;
  const cpuPercent = cpuValid
    ? Math.min(100, Math.max(0, nodeData!.cpuUsagePercent))
    : undefined;

  const memTotalValid =
    typeof nodeData?.memoryTotalMb === "number" && nodeData.memoryTotalMb > 0;
  const memUsedValid =
    typeof nodeData?.memoryUsedMb === "number" && nodeData.memoryUsedMb >= 0;
  const memPercent =
    memTotalValid && memUsedValid
      ? Math.min(
          100,
          Math.max(0, (nodeData!.memoryUsedMb / nodeData!.memoryTotalMb) * 100),
        )
      : undefined;
  const memAvailableMb =
    memTotalValid && memUsedValid
      ? Math.max(0, nodeData!.memoryTotalMb - nodeData!.memoryUsedMb)
      : undefined;

  const diskTotalValid =
    typeof nodeData?.diskTotalMb === "number" && nodeData.diskTotalMb > 0;
  const diskUsedValid =
    typeof nodeData?.diskUsedMb === "number" && nodeData.diskUsedMb >= 0;
  const diskPercent =
    diskTotalValid && diskUsedValid
      ? Math.min(
          100,
          Math.max(0, (nodeData!.diskUsedMb / nodeData!.diskTotalMb) * 100),
        )
      : undefined;
  const diskAvailableMb =
    diskTotalValid && diskUsedValid
      ? Math.max(0, nodeData!.diskTotalMb - nodeData!.diskUsedMb)
      : undefined;

  const gpuCount = nodeData?.gpuCount;
  const gpuDetected = typeof gpuCount === "number" && gpuCount > 0;

  // Initial Loading Skeleton
  if (loading && !nodeData) {
    return (
      <div className="space-y-4 max-w-7xl">
        <div className="flex items-center justify-between pb-2 border-b border-console-border">
          <Skeleton className="h-6 w-36" />
          <div className="flex items-center gap-2">
            <Skeleton className="h-7 w-20" />
            <Skeleton className="h-7 w-36" />
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <Skeleton className="h-48" />
          <Skeleton className="h-48" />
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
            Node Status
          </h1>
          <StatusBadge status={status?.lifecycleState || "ONLINE"} />
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleExportDiagnostics}
            disabled={!nodeData}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors disabled:opacity-50"
            title="Export JSON diagnostics"
          >
            <Download className="w-3.5 h-3.5 text-console-textDim" />
            <span>Export Diagnostics</span>
          </button>

          <button
            type="button"
            onClick={() => refetch()}
            disabled={refreshing}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors disabled:opacity-50"
            title="Refresh node status"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 text-console-textDim ${refreshing ? "animate-spin" : ""}`}
            />
            <span>Refresh</span>
          </button>
        </div>
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

      {/* 2. Primary 2-Column Details Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Left Column: Node Identity, CPU Architecture */}
        <div className="space-y-4">
          {/* Node Identity */}
          <ConsoleCard title="Node Identity">
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
                      title="Click to copy Worker ID"
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
                  label: "Operating System",
                  value: nodeData?.osName
                    ? `${nodeData.osName} ${nodeData.osVersion || ""}`.trim()
                    : "Unavailable",
                },
                {
                  label: "Architecture",
                  value: nodeData?.architecture || "Unavailable",
                },
                {
                  label: "Agent Version",
                  value: nodeData?.agentVersion || "Unavailable",
                },
              ]}
            />
          </ConsoleCard>

          {/* CPU Architecture & Utilization */}
          <ConsoleCard title="Processor (CPU)">
            <div className="space-y-3.5">
              {/* Compact Utilization Meter */}
              <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm space-y-2">
                <div className="flex items-baseline justify-between text-xs">
                  <span className="text-console-textDim font-medium uppercase tracking-wider text-[11px]">
                    CPU Utilization
                  </span>
                  <span className="font-mono font-semibold text-console-text">
                    {cpuPercent !== undefined
                      ? `${cpuPercent.toFixed(1)}%`
                      : "Unavailable"}
                  </span>
                </div>

                <div className="w-full bg-slate-200 h-2 rounded-sm overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${
                      cpuPercent && cpuPercent >= 90
                        ? "bg-rose-500"
                        : cpuPercent && cpuPercent >= 75
                          ? "bg-amber-500"
                          : "bg-console-accent"
                    }`}
                    style={{ width: `${cpuPercent ?? 0}%` }}
                  />
                </div>
              </div>

              <KeyValueTable
                columns={1}
                items={[
                  {
                    label: "Model",
                    value: nodeData?.cpuModel || "Unavailable",
                  },
                  {
                    label: "Cores",
                    value: homeData?.cpuCores
                      ? `${homeData.cpuCores} cores`
                      : "Unavailable",
                  },
                ]}
              />
            </div>
          </ConsoleCard>
        </div>

        {/* Right Column: Memory, Storage, Network & GPU */}
        <div className="space-y-4">
          {/* Memory */}
          <ConsoleCard title="Memory (RAM)">
            <div className="space-y-3.5">
              {/* Utilization Meter */}
              <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm space-y-2">
                <div className="flex items-baseline justify-between text-xs">
                  <span className="text-console-textDim font-medium uppercase tracking-wider text-[11px]">
                    Memory Utilization
                  </span>
                  <span className="font-mono font-semibold text-console-text">
                    {memPercent !== undefined
                      ? `${memPercent.toFixed(1)}%`
                      : "Unavailable"}
                  </span>
                </div>

                <div className="w-full bg-slate-200 h-2 rounded-sm overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${
                      memPercent && memPercent >= 90
                        ? "bg-rose-500"
                        : memPercent && memPercent >= 75
                          ? "bg-amber-500"
                          : "bg-console-accent"
                    }`}
                    style={{ width: `${memPercent ?? 0}%` }}
                  />
                </div>
              </div>

              <KeyValueTable
                columns={1}
                items={[
                  {
                    label: "Used Memory",
                    value: formatMb(nodeData?.memoryUsedMb),
                  },
                  {
                    label: "Available Memory",
                    value: formatMb(memAvailableMb),
                  },
                  {
                    label: "Total Memory",
                    value: formatMb(nodeData?.memoryTotalMb),
                  },
                ]}
              />
            </div>
          </ConsoleCard>

          {/* Storage */}
          <ConsoleCard title="Storage (Disk)">
            <div className="space-y-3.5">
              {/* Utilization Meter */}
              <div className="p-3 bg-console-subtle border border-console-borderSubtle rounded-sm space-y-2">
                <div className="flex items-baseline justify-between text-xs">
                  <span className="text-console-textDim font-medium uppercase tracking-wider text-[11px]">
                    Storage Utilization
                  </span>
                  <span className="font-mono font-semibold text-console-text">
                    {diskPercent !== undefined
                      ? `${diskPercent.toFixed(1)}%`
                      : "Unavailable"}
                  </span>
                </div>

                <div className="w-full bg-slate-200 h-2 rounded-sm overflow-hidden">
                  <div
                    className={`h-full transition-all duration-300 ${
                      diskPercent && diskPercent >= 90
                        ? "bg-rose-500"
                        : diskPercent && diskPercent >= 75
                          ? "bg-amber-500"
                          : "bg-console-accent"
                    }`}
                    style={{ width: `${diskPercent ?? 0}%` }}
                  />
                </div>
              </div>

              <KeyValueTable
                columns={1}
                items={[
                  {
                    label: "Used Storage",
                    value: formatMb(nodeData?.diskUsedMb),
                  },
                  {
                    label: "Available Storage",
                    value: formatMb(diskAvailableMb),
                  },
                  {
                    label: "Total Storage",
                    value: formatMb(nodeData?.diskTotalMb),
                  },
                ]}
              />
            </div>
          </ConsoleCard>

          {/* Network & GPU Telemetry */}
          <ConsoleCard title="Network & Acceleration">
            <KeyValueTable
              columns={1}
              items={[
                {
                  label: "Bytes Sent",
                  mono: true,
                  value: formatBytes(nodeData?.networkBytesSent),
                },
                {
                  label: "Bytes Received",
                  mono: true,
                  value: formatBytes(nodeData?.networkBytesReceived),
                },
                {
                  label: "GPU",
                  value: gpuDetected ? (
                    <span className="text-emerald-700 font-mono font-medium">
                      {gpuCount} unit{gpuCount > 1 ? "s" : ""} detected
                    </span>
                  ) : (
                    <span className="text-console-textDim font-medium">
                      Not detected
                    </span>
                  ),
                },
              ]}
            />
          </ConsoleCard>
        </div>
      </div>
    </div>
  );
}
