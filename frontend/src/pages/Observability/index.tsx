import { Cpu, MemoryStick, Zap, HardDrive, Network, Server } from 'lucide-react';
import { AreaChartWidget } from '../../components/charts/AreaChartWidget';
import { DonutChart } from '../../components/charts/DonutChart';
import { BarChartWidget } from '../../components/charts/BarChartWidget';
import { LoadingSkeleton } from '../../components/feedback/LoadingSkeleton';
import { ErrorBanner } from '../../components/feedback/ErrorBanner';
import { useClusterSummary } from '../../hooks/useCluster';
import { useWorkers } from '../../hooks/useWorkers';
import { mockChartData } from '../../mock/cluster';

function SectionHeader({ icon, title, subtitle }: { icon: React.ReactNode; title: string; subtitle?: string }) {
  return (
    <div className="flex items-center gap-3 mb-5">
      <div className="w-9 h-9 rounded-lg bg-accent-500/10 border border-accent-500/20 flex items-center justify-center flex-shrink-0">
        <span className="text-accent-400">{icon}</span>
      </div>
      <div>
        <h2 className="text-sm font-bold text-slate-200">{title}</h2>
        {subtitle && <p className="text-xs text-slate-600">{subtitle}</p>}
      </div>
    </div>
  );
}

function MetricBox({ label, value, sub, highlight = false }: {
  label: string; value: string | number; sub?: string; highlight?: boolean;
}) {
  return (
    <div className={`p-4 rounded-xl border ${highlight
      ? 'bg-accent-500/5 border-accent-500/20'
      : 'bg-surface-800 border-slate-800'
    }`}>
      <p className="section-title mb-1.5">{label}</p>
      <p className={`text-xl font-bold ${highlight ? 'gradient-text' : 'text-slate-100'}`}>{value}</p>
      {sub && <p className="text-xs text-slate-600 mt-1">{sub}</p>}
    </div>
  );
}

export default function Observability() {
  const { data: s, isLoading: clusterLoading, isError: clusterError, refetch: refetchCluster } = useClusterSummary();
  const { data: workers = [], isLoading: workersLoading, isError: workersError, refetch: refetchWorkers } = useWorkers();

  const cpuPct = s?.cpuUsagePercent ?? 0;
  const memPct = s?.memoryUsagePercent ?? 0;
  const storagePct = s?.storageUsagePercent ?? 0;
  const gpuPct = (s?.totalGpus ?? 0) > 0 ? Math.round(((s?.usedGpus ?? 0) / (s?.totalGpus ?? 1)) * 100) : 0;

  // Per-worker CPU contribution
  const cpuContrib = workers.map(w => ({
    name: w.hostname,
    value: w.state === 'OFFLINE' ? 0 : w.cpuCores,
    color: w.state === 'OFFLINE' ? '#334155' : w.state === 'BUSY' ? '#f59e0b' : '#06b6d4',
  }));

  const memContrib = workers.map(w => ({
    name: w.hostname,
    value: w.state === 'OFFLINE' ? 0 : Math.round(w.memoryRamMb / 1024),
    color: w.state === 'OFFLINE' ? '#334155' : '#8b5cf6',
  }));

  return (
    <div className="p-6 space-y-8 animate-fade-in">
      {(clusterError || workersError) && (
        <ErrorBanner message="Failed to load observability data." onRetry={() => { refetchCluster(); refetchWorkers(); }} />
      )}

      {(clusterLoading || workersLoading) && (
        <div className="card p-6"><LoadingSkeleton rows={6} /></div>
      )}

      {s && !clusterLoading && !workersLoading && (
        <div className="space-y-8">

      {/* ── Hero: Logical Cluster ───────────────────────────────────────────── */}
      <div className="relative card overflow-hidden">
        {/* Background glow */}
        <div className="absolute inset-0 bg-hero-glow pointer-events-none" />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-72 h-1 bg-gradient-to-r from-transparent via-cyan-500/60 to-transparent" />

        <div className="relative px-8 py-10 text-center">
          <p className="section-title mb-3 tracking-[0.3em]">Distributed Compute Cluster</p>
          <h1 className="text-4xl font-extrabold gradient-text mb-2">Logical Cluster</h1>
          <p className="text-slate-500 text-sm mb-8">All physical workers aggregated as one compute unit</p>

          {/* Big metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-6 max-w-2xl mx-auto">
            {[
              { value: s.totalCpuCores, label: 'CPU CORES', icon: '⚙️' },
              { value: `${s.totalMemoryGb} GB`, label: 'TOTAL RAM', icon: '🧠' },
              { value: s.totalGpus, label: 'GPUs', icon: '🎮' },
              { value: s.connectedWorkers, label: 'WORKERS', icon: '🖥️' },
            ].map(m => (
              <div key={m.label} className="flex flex-col items-center gap-1">
                <span className="text-2xl mb-1">{m.icon}</span>
                <span className="text-3xl font-black gradient-text leading-none">{m.value}</span>
                <span className="text-[10px] font-semibold uppercase tracking-widest text-slate-500">{m.label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* ── Worker Contribution Diagram ─────────────────────────────────────── */}
      <div className="card p-6">
        <SectionHeader icon={<Server size={16} />} title="Worker Contribution" subtitle="Physical PCs → Logical Cluster" />

        <div className="flex flex-col items-center">
          {/* Cluster Node */}
          <div className="w-48 rounded-xl border border-accent-500/40 bg-accent-500/5 px-5 py-3 text-center mb-2 relative shadow-glow-cyan">
            <p className="text-xs font-semibold uppercase tracking-wider text-accent-400 mb-1">Logical Cluster</p>
            <p className="text-sm font-bold text-slate-100">{s.totalCpuCores} CPU · {s.totalMemoryGb} GB</p>
            <p className="text-xs text-slate-500">{s.totalGpus} GPU · {s.totalStorageTb} TB</p>
          </div>

          {/* Tree connector */}
          <div className="flex items-start justify-center gap-0 mt-0">
            <svg width="480" height="60" viewBox="0 0 480 60" className="overflow-visible">
              {/* Vertical from cluster */}
              <line x1="240" y1="0" x2="240" y2="20" stroke="#334155" strokeWidth="1.5" />
              {/* Horizontal bar */}
              <line x1="80" y1="20" x2="400" y2="20" stroke="#334155" strokeWidth="1.5" />
              {/* Down to each worker */}
              <line x1="80"  y1="20" x2="80"  y2="50" stroke="#334155" strokeWidth="1.5" />
              <line x1="240" y1="20" x2="240" y2="50" stroke="#334155" strokeWidth="1.5" />
              <line x1="400" y1="20" x2="400" y2="50" stroke="#334155" strokeWidth="1.5" />
            </svg>
          </div>

          {/* Workers */}
          <div className="grid grid-cols-3 gap-4 w-full max-w-xl -mt-2">
            {workers.map(w => {
              const offline = w.state === 'OFFLINE';
              return (
                <div key={w.id} className={`rounded-xl border px-4 py-3 text-center transition-all ${
                  offline
                    ? 'border-slate-800 bg-surface-800 opacity-50'
                    : w.state === 'BUSY'
                    ? 'border-amber-500/30 bg-amber-500/5'
                    : 'border-slate-700 bg-surface-800 hover:border-slate-600'
                }`}>
                  <div className="flex items-center justify-center gap-1.5 mb-2">
                    <span className={`w-1.5 h-1.5 rounded-full ${
                      offline ? 'bg-slate-600' : w.state === 'BUSY' ? 'bg-amber-400 animate-pulse' : 'bg-green-400 animate-pulse'
                    }`} />
                    <p className="text-xs font-bold text-slate-200">{w.hostname}</p>
                  </div>
                  <p className="text-[11px] text-slate-400">{offline ? '—' : `${w.cpuCores} CPU`}</p>
                  <p className="text-[11px] text-slate-400">{offline ? '—' : `${Math.round(w.memoryRamMb / 1024)} GB`}</p>
                  <p className="text-[11px] text-slate-600">{w.gpuCount > 0 ? `${w.gpuCount} GPUs` : 'No GPU'}</p>
                  {offline && <p className="text-[10px] text-red-400 mt-1">Offline</p>}
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* ── CPU ─────────────────────────────────────────────────────────────── */}
      <div className="card p-6">
        <SectionHeader icon={<Cpu size={16} />} title="CPU" subtitle="Aggregate across all workers" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          <MetricBox label="Total Cores"     value={s.totalCpuCores} highlight />
          <MetricBox label="Used Cores"      value={s.usedCpuCores} />
          <MetricBox label="Available"       value={s.availableCpuCores} />
          <MetricBox label="Utilization"     value={`${cpuPct.toFixed(1)}%`} />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div>
            <p className="section-title mb-3">Utilization History</p>
            <AreaChartWidget data={mockChartData.cpu} label="cpu" color="#06b6d4" height={140} showGrid />
          </div>
          <div>
            <p className="section-title mb-3">Per-Worker CPU Cores</p>
            <BarChartWidget data={cpuContrib} unit=" cores" height={140} layout="horizontal" />
          </div>
        </div>
      </div>

      {/* ── Memory ──────────────────────────────────────────────────────────── */}
      <div className="card p-6">
        <SectionHeader icon={<MemoryStick size={16} />} title="Memory" subtitle="Aggregate RAM across all workers" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div>
            <div className="grid grid-cols-2 gap-3 mb-6">
              <MetricBox label="Total RAM"   value={`${s.totalMemoryGb} GB`} highlight />
              <MetricBox label="Used"        value={`${s.usedMemoryGb} GB`} />
              <MetricBox label="Available"   value={`${s.availableMemoryGb} GB`} />
              <MetricBox label="Utilization" value={`${memPct.toFixed(1)}%`} />
            </div>
            <p className="section-title mb-3">Utilization History</p>
            <AreaChartWidget data={mockChartData.memory} label="memory" color="#8b5cf6" height={120} showGrid />
          </div>
          <div className="flex flex-col items-center justify-center gap-4">
            <DonutChart value={memPct} color="#8b5cf6" size={160} label={`${memPct.toFixed(1)}%`} sublabel="used" />
            <div>
              <p className="section-title mb-2 text-center">Per-Worker RAM</p>
              <BarChartWidget data={memContrib} unit=" GB" height={130} layout="horizontal" />
            </div>
          </div>
        </div>
      </div>

      {/* ── GPU ─────────────────────────────────────────────────────────────── */}
      <div className="card p-6">
        <SectionHeader icon={<Zap size={16} />} title="GPU" subtitle="Aggregate GPU availability" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          <MetricBox label="Total GPUs"     value={s.totalGpus} highlight />
          <MetricBox label="Available"      value={s.availableGpus} />
          <MetricBox label="Allocated"      value={s.usedGpus} />
          <MetricBox label="Utilization"    value={`${gpuPct}%`} />
        </div>
        <div className="space-y-2">
          <p className="section-title mb-3">GPU Inventory</p>
          {workers.map(w => (
            <div key={w.id} className="flex items-center gap-4 p-3 rounded-lg bg-surface-800 border border-slate-800">
              <div className="w-1.5 h-8 rounded-full flex-shrink-0" style={{
                backgroundColor: w.state === 'OFFLINE' ? '#334155' : w.gpuCount > 0 ? '#10b981' : '#334155'
              }} />
              <div className="flex-1">
                <p className="text-sm font-semibold text-slate-200">{w.hostname}</p>
                <p className="text-xs text-slate-500">{w.gpuCount > 0 ? `${w.gpuCount} GPUs` : 'No GPU'}</p>
              </div>
              {w.gpuCount > 0 && (
                <>
                  <div className="text-right">
                    <p className="text-xs text-slate-500">Usage</p>
                    <p className={`text-sm font-semibold ${w.state === 'OFFLINE' ? 'text-slate-600' : 'text-emerald-400'}`}>
                      {w.state === 'OFFLINE' ? '—' : 'Active'}
                    </p>
                  </div>
                </>
              )}
              {w.gpuCount === 0 && <p className="text-xs text-slate-600">Not equipped</p>}
            </div>
          ))}
        </div>
        {/* GPU history */}
        <div className="mt-6">
          <p className="section-title mb-3">GPU Utilization History</p>
          <AreaChartWidget data={mockChartData.gpu} label="gpu" color="#10b981" height={120} showGrid />
        </div>
      </div>

      {/* ── Storage ─────────────────────────────────────────────────────────── */}
      <div className="card p-6">
        <SectionHeader icon={<HardDrive size={16} />} title="Storage" subtitle="Aggregate logical storage — each worker's disk contributes independently" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
          <MetricBox label="Total Storage"  value={`${s.totalStorageTb} TB`} highlight />
          <MetricBox label="Used"           value={`${s.usedStorageTb.toFixed(1)} TB`} />
          <MetricBox label="Available"      value={`${s.availableStorageTb.toFixed(1)} TB`} />
          <MetricBox label="Utilization"    value={`${storagePct.toFixed(1)}%`} />
        </div>
        <div className="progress-bar h-3 mb-2">
          <div className="progress-fill bg-amber-500 h-3" style={{ width: `${storagePct}%` }} />
        </div>
        <p className="text-xs text-slate-600">
          Note: Aggregate storage represents the sum of individual worker disks, not a shared filesystem.
        </p>
      </div>

      {/* ── Network ─────────────────────────────────────────────────────────── */}
      <div className="card p-6">
        <SectionHeader icon={<Network size={16} />} title="Network" subtitle="Aggregate inbound/outbound across all workers" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
          <MetricBox label="Inbound"         value={`${s.networkInboundMbps} Mbps`} highlight />
          <MetricBox label="Outbound"        value={`${s.networkOutboundMbps} Mbps`} />
          <MetricBox label="Throughput"      value={`${s.networkInboundMbps + s.networkOutboundMbps} Mbps`} />
          <MetricBox label="Health"          value="✓ Connected" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div>
            <p className="section-title mb-3">Inbound Traffic</p>
            <AreaChartWidget data={mockChartData.networkIn} label="netIn" color="#f59e0b" unit=" Mbps" height={120} showGrid />
          </div>
          <div>
            <p className="section-title mb-3">Outbound Traffic</p>
            <AreaChartWidget data={mockChartData.networkOut} label="netOut" color="#fb923c" unit=" Mbps" height={120} showGrid />
          </div>
        </div>
      </div>
        </div>
      )}
    </div>
  );
}
