import type { Worker } from '../../types';
import { StatusBadge } from '../../components/status/StatusBadge';
import { Cpu, MemoryStick, HardDrive } from 'lucide-react';

interface WorkerCardProps {
  worker: Worker;
  onClick: (worker: Worker) => void;
}

function MeterRow({
  icon, label, value, pct, colorClass
}: {
  icon: React.ReactNode; label: string; value: string; max?: string; pct: number; colorClass: string;
}) {
  return (
    <div className="mb-2.5">
      <div className="flex items-center justify-between mb-1">
        <div className="flex items-center gap-1.5 text-slate-500">
          {icon}
          <span className="text-[11px]">{label}</span>
        </div>
        <span className="text-[11px] text-slate-400">{value}</span>
      </div>
      <div className="progress-bar">
        <div className={`progress-fill ${colorClass}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export function WorkerCard({ worker, onClick }: WorkerCardProps) {
  const isOffline = worker.status === 'OFFLINE';
  const hbAge = Math.round((Date.now() - new Date(worker.lastHeartbeat).getTime()) / 1000);

  const cpuPct = worker.cpu.usagePercent;
  const memPct = worker.memory.usagePercent;
  const storagePct = worker.storage.usagePercent;

  return (
    <div
      onClick={() => onClick(worker)}
      className="card-hover p-5 cursor-pointer animate-fade-in"
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div>
          <h3 className="text-base font-bold text-slate-100">{worker.hostname}</h3>
          <p className="text-xs text-slate-500 font-mono mt-0.5">{worker.ipAddress}</p>
        </div>
        <StatusBadge status={worker.status} />
      </div>

      {/* OS + arch */}
      <p className="text-[11px] text-slate-600 mb-4 truncate">{worker.os} · {worker.architecture}</p>

      {/* Meters */}
      {isOffline ? (
        <div className="py-4 text-center">
          <p className="text-xs text-slate-600">Worker is offline</p>
          <p className="text-xs text-red-400 mt-1 font-mono">{Math.round(hbAge / 60)}m since last heartbeat</p>
        </div>
      ) : (
        <>
          <MeterRow
            icon={<Cpu size={11} />}
            label="CPU"
            value={`${cpuPct}% · ${worker.cpu.cores} cores`}
            pct={cpuPct}
            colorClass={cpuPct > 85 ? 'bg-red-500' : cpuPct > 60 ? 'bg-amber-500' : 'bg-cyan-500'}
          />
          <MeterRow
            icon={<MemoryStick size={11} />}
            label="RAM"
            value={`${worker.memory.usedGb}/${worker.memory.totalGb} GB`}
            pct={memPct}
            colorClass={memPct > 85 ? 'bg-red-500' : 'bg-violet-500'}
          />
          <MeterRow
            icon={<HardDrive size={11} />}
            label="Storage"
            value={`${worker.storage.usedTb.toFixed(1)}/${worker.storage.totalTb} TB`}
            pct={storagePct}
            colorClass="bg-amber-500"
          />
        </>
      )}

      {/* Footer */}
      <div className="mt-4 pt-3 border-t border-slate-800 grid grid-cols-2 gap-2">
        <div>
          <p className="text-[10px] text-slate-600 mb-0.5">GPU</p>
          <p className="text-xs text-slate-400 truncate">
            {worker.gpu ? worker.gpu.model.replace('NVIDIA ', '') : 'None'}
          </p>
        </div>
        <div>
          <p className="text-[10px] text-slate-600 mb-0.5">Heartbeat</p>
          <p className={`text-xs font-mono ${isOffline ? 'text-red-400' : 'text-slate-400'}`}>
            {isOffline ? `${Math.round(hbAge / 60)}m ago` : `${hbAge}s ago`}
          </p>
        </div>
        <div>
          <p className="text-[10px] text-slate-600 mb-0.5">Agent</p>
          <p className="text-xs text-slate-500 font-mono">{worker.agentVersion}</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-600 mb-0.5">Network</p>
          <p className="text-xs text-slate-400">
            {isOffline ? '—' : `↓ ${worker.network.inboundMbps} Mbps`}
          </p>
        </div>
      </div>
    </div>
  );
}
