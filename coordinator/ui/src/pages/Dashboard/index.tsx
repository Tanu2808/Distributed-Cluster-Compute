import { useQuery } from "@tanstack/react-query";
import {
  Cpu,
  MemoryStick,
  Zap,
  HardDrive,
} from "lucide-react";

import { ErrorBanner } from "../../components/feedback/ErrorBanner";
import { useClusterSummary } from "../../hooks/useCluster";
import { useWorkers } from "../../hooks/useWorkers";
import { useClusterEvents } from "../../hooks/useEvents";
import type { ClusterEvent } from "../../types";

const eventSeverityStyles: Record<string, string> = {
  INFO: "text-slate-400",
  SUCCESS: "text-green-400",
  WARNING: "text-amber-400",
  ERROR: "text-red-400",
};

export default function Dashboard() {
  const {
    data: cluster,
    isLoading: clusterLoading,
    isError: clusterError,
    refetch: refetchCluster,
  } = useClusterSummary();

  const {
    data: workers = [],
    isLoading: workersLoading,
    isError: workersError,
    refetch: refetchWorkers,
  } = useWorkers();

  const {
    data: events = [],
    isLoading: eventsLoading,
    isError: eventsError,
    refetch: refetchEvents,
  } = useClusterEvents();

  const { data: jobs = [], isLoading: jobsLoading } = useQuery({
    queryKey: ["jobs"],
    queryFn: async () => {
      const res = await fetch("/api/jobs");
      if (!res.ok) throw new Error("Failed to load jobs");
      return res.json() as Promise<any[]>;
    },
  });

  const activeJobs = jobs.filter((j) => j.state === "RUNNING");

  const getStatusColor = (status: string) => {
    switch (status) {
      case "RUNNING":
      case "ONLINE":
        return "text-green-400";
      case "DEGRADED":
        return "text-amber-400";
      case "NO WORKERS":
      case "OFFLINE":
        return "text-slate-400";
      default:
        return "text-slate-300";
    }
  };

  return (
    <div className="p-6 space-y-6 max-w-7xl mx-auto">
      {(clusterError || workersError || eventsError) && (
        <ErrorBanner
          message="Failed to load some cluster data. Check connection."
          onRetry={() => {
            refetchCluster();
            refetchWorkers();
            refetchEvents();
          }}
        />
      )}

      {/* HEADER */}
      <div className="flex flex-col border-b border-slate-800 pb-4">
        <h1 className="text-xl font-semibold text-slate-100">Cluster Overview</h1>
        {clusterLoading ? (
          <div className="mt-2 w-48 h-4 bg-slate-800 animate-pulse rounded" />
        ) : (
          <div className="flex gap-6 mt-3 text-sm">
            <div>
              <span className="text-slate-500 mr-2">Status:</span>
              <span className={`font-semibold ${getStatusColor(cluster?.status ?? "UNKNOWN")}`}>
                ● {cluster?.status ?? "UNKNOWN"}
              </span>
            </div>
            <div>
              <span className="text-slate-500 mr-2">Coordinator:</span>
              <span className={`font-semibold ${getStatusColor(cluster?.coordinatorStatus ?? "UNKNOWN")}`}>
                {cluster?.coordinatorStatus ?? "UNKNOWN"}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* CLUSTER SUMMARY */}
      <div>
        <h2 className="text-sm font-semibold text-slate-100 mb-3 uppercase tracking-wider">Cluster Summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-surface-800 border border-slate-700 p-4 rounded text-sm">
            <div className="text-slate-400 mb-1">Workers</div>
            <div className="text-lg text-slate-200">{cluster?.connectedWorkers ?? 0} ONLINE</div>
          </div>
          <div className="bg-surface-800 border border-slate-700 p-4 rounded text-sm">
            <div className="text-slate-400 mb-1">Tasks</div>
            <div className="text-lg text-slate-200">
              {workers.reduce((acc, w) => acc + (w.activeTasks || 0), 0)} RUNNING
            </div>
          </div>
          <div className="bg-surface-800 border border-slate-700 p-4 rounded text-sm">
            <div className="text-slate-400 mb-1">Jobs</div>
            <div className="text-lg text-slate-200">{activeJobs.length} RUNNING</div>
          </div>
          <div className="bg-surface-800 border border-slate-700 p-4 rounded text-sm">
            <div className="text-slate-400 mb-1">Cluster Utilization</div>
            <div className="text-lg text-slate-200">{cluster?.cpuUsagePercent?.toFixed(1) ?? 0}%</div>
          </div>
        </div>
      </div>

      {/* RESOURCE SUMMARY */}
      <div>
        <h2 className="text-sm font-semibold text-slate-100 mb-3 uppercase tracking-wider">Resource Summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-surface-800 border border-slate-700 p-4 rounded flex items-center gap-3">
            <Cpu className="text-slate-500" size={20} />
            <div>
              <div className="text-xs text-slate-400 uppercase">CPU</div>
              <div className="text-sm text-slate-200">{cluster?.usedCpuCores ?? 0} / {cluster?.totalCpuCores ?? 0} cores</div>
            </div>
          </div>
          <div className="bg-surface-800 border border-slate-700 p-4 rounded flex items-center gap-3">
            <MemoryStick className="text-slate-500" size={20} />
            <div>
              <div className="text-xs text-slate-400 uppercase">Memory</div>
              <div className="text-sm text-slate-200">{cluster?.usedMemoryGb ?? 0} / {cluster?.totalMemoryGb ?? 0} GB</div>
            </div>
          </div>
          <div className="bg-surface-800 border border-slate-700 p-4 rounded flex items-center gap-3">
            <Zap className="text-slate-500" size={20} />
            <div>
              <div className="text-xs text-slate-400 uppercase">GPU</div>
              <div className="text-sm text-slate-200">{cluster?.usedGpus ?? 0} / {cluster?.totalGpus ?? 0}</div>
            </div>
          </div>
          {cluster && cluster.totalStorageTb > 0 && (
            <div className="bg-surface-800 border border-slate-700 p-4 rounded flex items-center gap-3">
              <HardDrive className="text-slate-500" size={20} />
              <div>
                <div className="text-xs text-slate-400 uppercase">Storage</div>
                <div className="text-sm text-slate-200">{cluster?.usedStorageTb?.toFixed(1) ?? 0} / {cluster?.totalStorageTb ?? 0} TB</div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* WORKER STATUS TABLE */}
      <div>
        <h2 className="text-sm font-semibold text-slate-100 mb-3 uppercase tracking-wider">Worker Status</h2>
        <div className="bg-surface-800 border border-slate-700 rounded overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-800/50 text-xs uppercase text-slate-400">
              <tr>
                <th className="px-4 py-2 font-medium">Worker ID</th>
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium">CPU</th>
                <th className="px-4 py-2 font-medium">Memory</th>
                <th className="px-4 py-2 font-medium">Active Tasks</th>
                <th className="px-4 py-2 font-medium">Last Seen</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700/50">
              {workersLoading ? (
                <tr><td colSpan={6} className="px-4 py-4 text-center text-slate-500">Loading...</td></tr>
              ) : workers.length === 0 ? (
                <tr><td colSpan={6} className="px-4 py-4 text-center text-slate-500">No workers connected</td></tr>
              ) : (
                workers.map((w) => {
                  const isOffline = w.state === "OFFLINE";
                  const hbAge = Math.round((Date.now() - new Date(w.lastHeartbeat).getTime()) / 1000);
                  const cpuUsed = Math.round((w.cpuCores * (w.cpuUsagePercent || 0)) / 100);
                  const memUsed = Math.round((w.memoryRamMb * (w.memoryUsagePercent || 0)) / 100000);
                  const memTotal = Math.round(w.memoryRamMb / 1024);
                  return (
                    <tr key={w.id} className="hover:bg-slate-700/20">
                      <td className="px-4 py-2 font-mono text-xs">{w.hostname} &middot; {w.id.substring(0, 8)}</td>
                      <td className={`px-4 py-2 font-semibold ${getStatusColor(w.state)}`}>{w.state}</td>
                      <td className="px-4 py-2 text-slate-300">{isOffline ? "—" : `${cpuUsed} / ${w.cpuCores}`}</td>
                      <td className="px-4 py-2 text-slate-300">{isOffline ? "—" : `${memUsed} / ${memTotal} GB`}</td>
                      <td className="px-4 py-2 text-slate-300">{w.activeTasks || 0}</td>
                      <td className="px-4 py-2 text-slate-400 text-xs">{hbAge}s ago</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ACTIVE JOBS */}
      <div>
        <h2 className="text-sm font-semibold text-slate-100 mb-3 uppercase tracking-wider">Active Jobs</h2>
        <div className="bg-surface-800 border border-slate-700 rounded overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-800/50 text-xs uppercase text-slate-400">
              <tr>
                <th className="px-4 py-2 font-medium">Job ID</th>
                <th className="px-4 py-2 font-medium">Type</th>
                <th className="px-4 py-2 font-medium">Status</th>
                <th className="px-4 py-2 font-medium">Progress</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700/50">
              {jobsLoading ? (
                <tr><td colSpan={4} className="px-4 py-4 text-center text-slate-500">Loading...</td></tr>
              ) : activeJobs.length === 0 ? (
                <tr><td colSpan={4} className="px-4 py-4 text-center text-slate-500">No active jobs</td></tr>
              ) : (
                activeJobs.map((j) => (
                  <tr key={j.jobId} className="hover:bg-slate-700/20">
                    <td className="px-4 py-2 font-mono text-xs">{j.jobId}</td>
                    <td className="px-4 py-2 text-slate-300">{j.taskType}</td>
                    <td className="px-4 py-2 text-green-400 font-semibold">{j.state}</td>
                    <td className="px-4 py-2 text-slate-300">{j.completedPartitions} / {j.totalPartitions}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* RECENT RUNTIME EVENTS */}
      <div>
        <h2 className="text-sm font-semibold text-slate-100 mb-3 uppercase tracking-wider">Recent Runtime Events</h2>
        <div className="bg-surface-800 border border-slate-700 rounded overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-800/50 text-xs uppercase text-slate-400">
              <tr>
                <th className="px-4 py-2 font-medium">Timestamp</th>
                <th className="px-4 py-2 font-medium">Type</th>
                <th className="px-4 py-2 font-medium">Severity</th>
                <th className="px-4 py-2 font-medium w-1/2">Message</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700/50">
              {eventsLoading ? (
                <tr><td colSpan={4} className="px-4 py-4 text-center text-slate-500">Loading...</td></tr>
              ) : events.length === 0 ? (
                <tr><td colSpan={4} className="px-4 py-4 text-center text-slate-500">No runtime events</td></tr>
              ) : (
                [...events].reverse().slice(0, 10).map((evt: ClusterEvent) => {
                  const ts = new Date(evt.timestamp);
                  return (
                    <tr key={evt.id} className="hover:bg-slate-700/20">
                      <td className="px-4 py-2 text-slate-400 text-xs font-mono">{ts.toISOString()}</td>
                      <td className="px-4 py-2 text-slate-300 text-xs">{evt.type}</td>
                      <td className={`px-4 py-2 text-xs font-semibold ${eventSeverityStyles[evt.severity]}`}>{evt.severity}</td>
                      <td className="px-4 py-2 text-slate-300">{evt.message}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
