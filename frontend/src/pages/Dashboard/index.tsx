import { useState } from "react";
import {
  Cpu,
  MemoryStick,
  Zap,
  HardDrive,
  Server,
  Activity,
} from "lucide-react";
import { ResourceCard } from "../../components/cards/ResourceCard";
import { AreaChartWidget } from "../../components/charts/AreaChartWidget";
import { WorkerTable } from "../../components/tables/WorkerTable";
import { WorkerDetailModal } from "../../components/modals/WorkerDetailModal";
import {
  CardSkeleton,
  LoadingSkeleton,
} from "../../components/feedback/LoadingSkeleton";
import { ErrorBanner } from "../../components/feedback/ErrorBanner";
import { useClusterSummary } from "../../hooks/useCluster";
import { useWorkers } from "../../hooks/useWorkers";
import { useClusterEvents } from "../../hooks/useEvents";
import { mockChartData } from "../../mock/cluster";
import type { Worker, ClusterEvent } from "../../types";

const eventSeverityStyles: Record<string, string> = {
  INFO: "bg-blue-500/10 text-blue-400 border-blue-500/20",
  SUCCESS: "bg-green-500/10 text-green-400 border-green-500/20",
  WARNING: "bg-amber-500/10 text-amber-400 border-amber-500/20",
  ERROR: "bg-red-500/10 text-red-400 border-red-500/20",
};

const eventTypeIcon: Record<string, string> = {
  WORKER_CONNECTED: "🟢",
  WORKER_DISCONNECTED: "🔴",
  HEARTBEAT_LOST: "⚠️",
  RESOURCE_CHANGED: "📊",
  COORDINATOR_STARTED: "🚀",
  CONFIG_CHANGED: "⚙️",
  WORKER_UNHEALTHY: "🔴",
  WORKER_RECOVERED: "✅",
};

function EventItem({ event }: { event: ClusterEvent }) {
  const ageMs = Date.now() - new Date(event.timestamp).getTime();
  const ageMin = Math.round(ageMs / 60000);
  const ageLabel =
    ageMin < 60 ? `${ageMin}m ago` : `${Math.round(ageMin / 60)}h ago`;

  return (
    <div className="flex items-start gap-3 py-3 border-b border-slate-800/70 last:border-0">
      <span className="text-sm mt-0.5 flex-shrink-0">
        {eventTypeIcon[event.type] ?? "ℹ️"}
      </span>
      <div className="flex-1 min-w-0">
        <p className="text-xs text-slate-300 leading-relaxed">
          {event.message}
        </p>
        <div className="flex items-center gap-2 mt-1">
          <span
            className={`text-[10px] px-1.5 py-0.5 rounded-full border ${eventSeverityStyles[event.severity]}`}
          >
            {event.severity}
          </span>
          <span className="text-[10px] text-slate-600">{ageLabel}</span>
        </div>
      </div>
    </div>
  );
}

export default function Dashboard() {
  const [selectedWorker, setSelectedWorker] = useState<Worker | null>(null);

  const {
    data: cluster,
    isLoading: clusterLoading,
    isError: clusterError,
    error: clusterErr,
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

  return (
    <div className="p-6 space-y-6 animate-fade-in">
      {/* Error banners */}
      {clusterError && (
        <ErrorBanner
          message={`Failed to load cluster data: ${clusterErr?.message ?? "Unknown error"}`}
          onRetry={() => refetchCluster()}
        />
      )}
      {workersError && (
        <ErrorBanner
          message="Failed to load workers."
          onRetry={() => refetchWorkers()}
        />
      )}
      {eventsError && (
        <ErrorBanner
          message="Failed to load events."
          onRetry={() => refetchEvents()}
        />
      )}

      {/* Cluster Status Strip */}
      {clusterLoading ? (
        <div className="card p-4">
          <LoadingSkeleton rows={1} />
        </div>
      ) : cluster ? (
        <div className="card p-4">
          <div className="flex flex-wrap items-center gap-6">
            <div>
              <p className="section-title mb-1">Cluster</p>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
                <span className="text-sm font-semibold text-green-400">
                  {cluster.status}
                </span>
              </div>
            </div>
            <div className="w-px h-8 bg-slate-800" />
            <div>
              <p className="section-title mb-1">Coordinator</p>
              <div className="flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-green-400" />
                <span className="text-sm font-semibold text-slate-200">
                  {cluster.coordinatorStatus}
                </span>
              </div>
            </div>
            <div className="w-px h-8 bg-slate-800" />
            <div>
              <p className="section-title mb-1">Workers</p>
              <p className="text-sm font-semibold text-slate-200">
                {cluster.totalWorkers} total
              </p>
            </div>
            <div>
              <p className="section-title mb-1">Connected</p>
              <p className="text-sm font-semibold text-green-400">
                {cluster.connectedWorkers}
              </p>
            </div>
            <div>
              <p className="section-title mb-1">Active</p>
              <p className="text-sm font-semibold text-amber-400">
                {cluster.activeWorkers}
              </p>
            </div>
            <div>
              <p className="section-title mb-1">Offline</p>
              <p className="text-sm font-semibold text-slate-500">
                {cluster.offlineWorkers}
              </p>
            </div>
          </div>
        </div>
      ) : null}

      {/* Aggregated Resources */}
      <div>
        <h2 className="section-title mb-3">Aggregated Cluster Resources</h2>
        {clusterLoading ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[0, 1, 2, 3].map((i) => (
              <CardSkeleton key={i} />
            ))}
          </div>
        ) : cluster ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <ResourceCard
              label="CPU Cores"
              icon={<Cpu size={15} />}
              used={cluster.usedCpuCores}
              total={cluster.totalCpuCores}
              usedLabel={`${cluster.usedCpuCores} cores`}
              totalLabel={`${cluster.totalCpuCores} cores`}
            />
            <ResourceCard
              label="Memory"
              icon={<MemoryStick size={15} />}
              used={cluster.usedMemoryGb}
              total={cluster.totalMemoryGb}
              usedLabel={`${cluster.usedMemoryGb} GB`}
              totalLabel={`${cluster.totalMemoryGb} GB`}
              colorClass="bg-violet-500"
            />
            <ResourceCard
              label="GPUs"
              icon={<Zap size={15} />}
              used={cluster.usedGpus}
              total={cluster.totalGpus}
              usedLabel={`${cluster.usedGpus} allocated`}
              totalLabel={`${cluster.totalGpus} total`}
              colorClass="bg-emerald-500"
            />
            <ResourceCard
              label="Storage"
              icon={<HardDrive size={15} />}
              used={cluster.usedStorageTb}
              total={cluster.totalStorageTb}
              usedLabel={`${cluster.usedStorageTb.toFixed(1)} TB`}
              totalLabel={`${cluster.totalStorageTb} TB`}
              colorClass="bg-amber-500"
            />
          </div>
        ) : null}
      </div>

      {/* Charts — use mock time-series until backend streams metrics history */}
      <div>
        <h2 className="section-title mb-3">Resource Utilization</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            {
              label: "CPU",
              data: mockChartData.cpu,
              color: "#06b6d4",
              unit: "%",
            },
            {
              label: "Memory",
              data: mockChartData.memory,
              color: "#8b5cf6",
              unit: "%",
            },
            {
              label: "GPU",
              data: mockChartData.gpu,
              color: "#10b981",
              unit: "%",
            },
            {
              label: "Network In",
              data: mockChartData.networkIn,
              color: "#f59e0b",
              unit: " Mbps",
            },
          ].map((chart) => (
            <div key={chart.label} className="card p-4">
              <div className="flex items-center justify-between mb-3">
                <p className="section-title">{chart.label}</p>
                <span className="text-lg font-bold text-slate-100">
                  {chart.data[chart.data.length - 1]?.value ?? 0}
                  {chart.unit}
                </span>
              </div>
              <AreaChartWidget
                data={chart.data}
                label={chart.label}
                color={chart.color}
                unit={chart.unit}
                height={100}
              />
            </div>
          ))}
        </div>
      </div>

      {/* Worker Table + Events */}
      <div className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        <div className="xl:col-span-2 card">
          <div className="flex items-center gap-2 px-5 py-4 border-b border-slate-800">
            <Server size={15} className="text-accent-400" />
            <h2 className="text-sm font-semibold text-slate-200">
              Worker Overview
            </h2>
            <span className="ml-auto text-xs text-slate-500">
              {workers.length} workers
            </span>
          </div>
          {workersLoading ? (
            <div className="p-5">
              <LoadingSkeleton rows={3} />
            </div>
          ) : (
            <WorkerTable workers={workers} onWorkerClick={setSelectedWorker} />
          )}
        </div>

        <div className="card">
          <div className="flex items-center gap-2 px-5 py-4 border-b border-slate-800">
            <Activity size={15} className="text-accent-400" />
            <h2 className="text-sm font-semibold text-slate-200">
              Recent Events
            </h2>
            <span className="ml-auto text-xs text-slate-500">
              {events.length} events
            </span>
          </div>
          <div className="px-5 py-2 max-h-96 overflow-y-auto">
            {eventsLoading ? (
              <div className="py-3">
                <LoadingSkeleton rows={4} />
              </div>
            ) : (
              [...events]
                .reverse()
                .map((evt) => <EventItem key={evt.id} event={evt} />)
            )}
          </div>
        </div>
      </div>

      <WorkerDetailModal
        worker={selectedWorker}
        onClose={() => setSelectedWorker(null)}
      />
    </div>
  );
}
