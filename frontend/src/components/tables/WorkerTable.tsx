import { useState } from 'react';
import { ChevronUp, ChevronDown, ChevronsUpDown } from 'lucide-react';
import type { Worker } from '../../types';
import { StatusBadge } from '../status/StatusBadge';

interface WorkerTableProps {
  workers: Worker[];
  onWorkerClick: (worker: Worker) => void;
}

type SortKey = 'hostname' | 'status' | 'cpu' | 'memory';
type SortDir = 'asc' | 'desc';

function MiniBar({ value, max, color }: { value: number; max: number; color: string }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="flex items-center gap-2">
      <div className="w-16 h-1.5 rounded-full bg-slate-800 overflow-hidden">
        <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="text-xs text-slate-400">{pct}%</span>
    </div>
  );
}

export function WorkerTable({ workers, onWorkerClick }: WorkerTableProps) {
  const [sortKey, setSortKey] = useState<SortKey>('hostname');
  const [sortDir, setSortDir] = useState<SortDir>('asc');

  function handleSort(key: SortKey) {
    if (key === sortKey) setSortDir(d => d === 'asc' ? 'desc' : 'asc');
    else { setSortKey(key); setSortDir('asc'); }
  }

  const sorted = [...workers].sort((a, b) => {
    let aVal: any, bVal: any;
    if (sortKey === 'hostname') { aVal = a.hostname; bVal = b.hostname; }
    else if (sortKey === 'status') { aVal = a.state; bVal = b.state; }
    else if (sortKey === 'cpu') { aVal = a.cpuUsagePercent; bVal = b.cpuUsagePercent; }
    else { aVal = a.memoryUsagePercent; bVal = b.memoryUsagePercent; }
    if (aVal < bVal) return sortDir === 'asc' ? -1 : 1;
    if (aVal > bVal) return sortDir === 'asc' ? 1 : -1;
    return 0;
  });

  function SortIcon({ col }: { col: SortKey }) {
    if (sortKey !== col) return <ChevronsUpDown size={12} className="text-slate-600" />;
    return sortDir === 'asc'
      ? <ChevronUp size={12} className="text-accent-400" />
      : <ChevronDown size={12} className="text-accent-400" />;
  }

  function SortableTh({ col, label }: { col: SortKey; label: string }) {
    return (
      <th
        className="py-3 px-4 text-left cursor-pointer select-none group"
        onClick={() => handleSort(col)}
      >
        <span className="flex items-center gap-1 text-xs font-semibold uppercase tracking-wider text-slate-500 group-hover:text-slate-400 transition-colors">
          {label} <SortIcon col={col} />
        </span>
      </th>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full data-table">
        <thead>
          <tr className="border-b border-slate-800">
            <SortableTh col="hostname" label="Worker" />
            <SortableTh col="status" label="Status" />
            <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">CPU</th>
            <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">RAM</th>
            <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">GPU</th>
            <th className="py-3 px-4 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">Heartbeat</th>
          </tr>
        </thead>
        <tbody>
          {sorted.map(worker => {
            const isOffline = worker.state === 'OFFLINE';
            const hbAge = Math.round((Date.now() - new Date(worker.lastHeartbeat).getTime()) / 1000);
            return (
              <tr
                key={worker.id}
                onClick={() => onWorkerClick(worker)}
                className="border-b border-slate-800/70 hover:bg-surface-800/50 transition-colors duration-100 cursor-pointer"
              >
                <td className="py-3 px-4">
                  <div>
                    <p className="text-sm font-semibold text-slate-200">{worker.hostname}</p>
                    <p className="text-xs text-slate-500 font-mono">{worker.ipAddress}</p>
                  </div>
                </td>
                <td className="py-3 px-4">
                  <StatusBadge status={worker.state} />
                </td>
                <td className="py-3 px-4">
                  {isOffline ? (
                    <span className="text-xs text-slate-600">—</span>
                  ) : (
                    <div>
                      <p className="text-xs text-slate-400 mb-1">
                        {Math.round(worker.cpuCores * worker.cpuUsagePercent / 100)}/{worker.cpuCores} cores
                      </p>
                      <MiniBar
                        value={worker.cpuUsagePercent}
                        max={100}
                        color={worker.cpuUsagePercent > 85 ? 'bg-red-500' : worker.cpuUsagePercent > 60 ? 'bg-amber-500' : 'bg-cyan-500'}
                      />
                    </div>
                  )}
                </td>
                <td className="py-3 px-4">
                  {isOffline ? (
                    <span className="text-xs text-slate-600">—</span>
                  ) : (
                    <div>
                      <p className="text-xs text-slate-400 mb-1">
                        {Math.round(worker.memoryRamMb * worker.memoryUsagePercent / 100000)}/{Math.round(worker.memoryRamMb / 1024)} GB
                      </p>
                      <MiniBar
                        value={worker.memoryUsagePercent}
                        max={100}
                        color={worker.memoryUsagePercent > 85 ? 'bg-red-500' : worker.memoryUsagePercent > 60 ? 'bg-amber-500' : 'bg-violet-500'}
                      />
                    </div>
                  )}
                </td>
                <td className="py-3 px-4">
                  {worker.gpuCount > 0 ? (
                    <span className="text-xs text-slate-300">{worker.gpuCount} GPUs</span>
                  ) : (
                    <span className="text-xs text-slate-600">None</span>
                  )}
                </td>
                <td className="py-3 px-4">
                  <span className={`text-xs font-mono ${isOffline ? 'text-red-400' : 'text-slate-400'}`}>
                    {isOffline ? `${Math.round(hbAge / 60)}m ago` : `${hbAge}s ago`}
                  </span>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
