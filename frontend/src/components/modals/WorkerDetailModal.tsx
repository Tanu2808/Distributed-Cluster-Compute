import { useEffect } from 'react';
import {
  X, Cpu,
  Clock, Activity, Wifi, Server, Tag
} from 'lucide-react';
import type { Worker } from '../../types';
import { StatusBadge } from '../status/StatusBadge';
import { mockEvents } from '../../mock/events';

interface WorkerDetailModalProps {
  worker: Worker | null;
  onClose: () => void;
}

function MeterBar({ value, max, label, color = 'bg-cyan-500' }: {
  value: number; max: number; label: string; color?: string;
}) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0;
  return (
    <div className="mb-3">
      <div className="flex justify-between items-baseline mb-1">
        <span className="text-xs text-slate-500">{label}</span>
        <span className="text-xs font-semibold text-slate-300">{pct}%</span>
      </div>
      <div className="progress-bar">
        <div className={`progress-fill ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function InfoRow({ label, value, mono = false }: { label: string; value: string | number; mono?: boolean }) {
  return (
    <div className="flex justify-between items-center py-2 border-b border-slate-800/70">
      <span className="text-xs text-slate-500">{label}</span>
      <span className={`text-xs text-slate-300 ${mono ? 'font-mono' : 'font-medium'}`}>{value}</span>
    </div>
  );
}

function SectionTitle({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <div className="flex items-center gap-2 mb-3 mt-5">
      <span className="text-accent-400">{icon}</span>
      <h3 className="text-xs font-semibold uppercase tracking-widest text-slate-400">{label}</h3>
    </div>
  );
}

export function WorkerDetailModal({ worker, onClose }: WorkerDetailModalProps) {
  useEffect(() => {
    if (!worker) return;
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [worker, onClose]);

  if (!worker) return null;

  const isOffline = worker.status === 'OFFLINE';
  const hbAgeSeconds = Math.round((Date.now() - new Date(worker.lastHeartbeat).getTime()) / 1000);
  const connectedAgeMs = Date.now() - new Date(worker.connectedSince).getTime();
  const connectedHours = Math.floor(connectedAgeMs / (1000 * 60 * 60));
  const connectedMins = Math.floor((connectedAgeMs % (1000 * 60 * 60)) / (1000 * 60));

  const workerEvents = mockEvents
    .filter(e => !e.workerId || e.workerId === worker.id)
    .slice(-5)
    .reverse();

  const eventIcon: Record<string, string> = {
    WORKER_CONNECTED:    '🟢',
    WORKER_DISCONNECTED: '🔴',
    HEARTBEAT_LOST:      '⚠️',
    RESOURCE_CHANGED:    '📊',
    COORDINATOR_STARTED: '🚀',
    CONFIG_CHANGED:      '⚙️',
    WORKER_UNHEALTHY:    '🔴',
    WORKER_RECOVERED:    '✅',
  };

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-40"
        onClick={onClose}
      />

      {/* Drawer */}
      <div className="fixed inset-y-0 right-0 z-50 w-full max-w-lg bg-[#0c1526] border-l border-slate-800 shadow-2xl animate-slide-in-right overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-[#0c1526]/95 backdrop-blur border-b border-slate-800 px-6 py-4 flex items-center justify-between z-10">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-lg bg-accent-500/10 border border-accent-500/20 flex items-center justify-center">
              <Server size={18} className="text-accent-400" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-slate-100">{worker.hostname}</h2>
              <p className="text-xs text-slate-500 font-mono">{worker.id}</p>
            </div>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={worker.status} />
            <button onClick={onClose} className="p-1.5 rounded-lg text-slate-500 hover:text-slate-300 hover:bg-slate-800 transition-colors">
              <X size={18} />
            </button>
          </div>
        </div>

        <div className="px-6 pb-8">
          {/* Connection Info */}
          <SectionTitle icon={<Wifi size={14} />} label="Connection" />
          <div className="card p-4">
            <InfoRow label="IP Address" value={worker.ipAddress} mono />
            <InfoRow label="Status" value={worker.status} />
            <InfoRow
              label="Last Heartbeat"
              value={isOffline ? `${Math.round(hbAgeSeconds / 60)} min ago` : `${hbAgeSeconds}s ago`}
            />
            <InfoRow
              label="Connected Since"
              value={isOffline ? 'Disconnected' : `${connectedHours}h ${connectedMins}m ago`}
            />
          </div>

          {/* Hardware Info */}
          <SectionTitle icon={<Cpu size={14} />} label="Hardware" />
          <div className="card p-4">
            <InfoRow label="CPU Model" value={worker.cpu.model} />
            <InfoRow label="CPU Cores / Threads" value={`${worker.cpu.cores} cores / ${worker.cpu.threads} threads`} />
            <InfoRow label="Total RAM" value={`${worker.memory.totalGb} GB`} />
            <InfoRow label="GPU" value={worker.gpu ? worker.gpu.model : 'Not Available'} />
            {worker.gpu && <InfoRow label="VRAM" value={`${worker.gpu.vramGb} GB`} />}
            <InfoRow label="Storage" value={`${worker.storage.totalTb} TB`} />
            <InfoRow label="OS" value={worker.os} />
            <InfoRow label="Architecture" value={worker.architecture} mono />
          </div>

          {/* Agent Info */}
          <SectionTitle icon={<Tag size={14} />} label="Agent" />
          <div className="card p-4">
            <InfoRow label="Agent Version" value={worker.agentVersion} mono />
          </div>

          {/* Resource Utilization */}
          {!isOffline && (
            <>
              <SectionTitle icon={<Activity size={14} />} label="Current Utilization" />
              <div className="card p-4">
                <MeterBar
                  label={`CPU — ${worker.cpu.usagePercent}% (${Math.round(worker.cpu.cores * worker.cpu.usagePercent / 100)}/${worker.cpu.cores} cores)`}
                  value={worker.cpu.usagePercent} max={100}
                  color={worker.cpu.usagePercent > 85 ? 'bg-red-500' : 'bg-cyan-500'}
                />
                <MeterBar
                  label={`Memory — ${worker.memory.usedGb}/${worker.memory.totalGb} GB`}
                  value={worker.memory.usedGb} max={worker.memory.totalGb}
                  color={worker.memory.usagePercent > 85 ? 'bg-red-500' : 'bg-violet-500'}
                />
                {worker.gpu && (
                  <MeterBar
                    label={`GPU — ${worker.gpu.usagePercent}% (${worker.gpu.model.replace('NVIDIA ', '')})`}
                    value={worker.gpu.usagePercent} max={100}
                    color="bg-emerald-500"
                  />
                )}
                <MeterBar
                  label={`Storage — ${worker.storage.usedTb.toFixed(1)}/${worker.storage.totalTb} TB`}
                  value={worker.storage.usedTb} max={worker.storage.totalTb}
                  color="bg-amber-500"
                />
                <div className="mt-3 pt-3 border-t border-slate-800 grid grid-cols-2 gap-3">
                  <div>
                    <p className="text-xs text-slate-500 mb-1">Network In</p>
                    <p className="text-sm font-semibold text-slate-200">{worker.network.inboundMbps} Mbps</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500 mb-1">Network Out</p>
                    <p className="text-sm font-semibold text-slate-200">{worker.network.outboundMbps} Mbps</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500 mb-1">Latency</p>
                    <p className="text-sm font-semibold text-slate-200">{worker.network.latencyMs} ms</p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-500 mb-1">Available Cores</p>
                    <p className="text-sm font-semibold text-slate-200">{worker.cpu.availableCores}</p>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Recent Events */}
          <SectionTitle icon={<Clock size={14} />} label="Recent Events" />
          <div className="space-y-2">
            {workerEvents.map(evt => {
              const ageMs = Date.now() - new Date(evt.timestamp).getTime();
              const ageMin = Math.round(ageMs / 60000);
              const ageLabel = ageMin < 60 ? `${ageMin}m ago` : `${Math.round(ageMin / 60)}h ago`;
              return (
                <div key={evt.id} className="card p-3 flex items-start gap-3">
                  <span className="text-sm mt-0.5">{eventIcon[evt.type] ?? 'ℹ️'}</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-slate-300 leading-relaxed">{evt.message}</p>
                    <p className="text-[10px] text-slate-600 mt-1">{ageLabel}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </>
  );
}
