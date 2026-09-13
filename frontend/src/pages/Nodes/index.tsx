import { useState } from 'react';
import { LayoutGrid, List, Search, Filter } from 'lucide-react';
import { WorkerCard } from './WorkerCard';
import { WorkerTable } from '../../components/tables/WorkerTable';
import { WorkerDetailModal } from '../../components/modals/WorkerDetailModal';
import { LoadingSkeleton } from '../../components/feedback/LoadingSkeleton';
import { ErrorBanner } from '../../components/feedback/ErrorBanner';
import { useWorkers } from '../../hooks/useWorkers';
import type { Worker, WorkerStatus } from '../../types';

const ALL_STATUSES: WorkerStatus[] = ['ONLINE', 'BUSY', 'OFFLINE', 'UNHEALTHY', 'REGISTERING', 'HEARTBEAT_TIMEOUT'];

export default function Nodes() {
  const [selectedWorker, setSelectedWorker] = useState<Worker | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<WorkerStatus | 'ALL'>('ALL');

  const { data: workers = [], isLoading, isError, refetch } = useWorkers();

  const filtered = workers.filter(w => {
    const matchesSearch =
      w.hostname.toLowerCase().includes(search.toLowerCase()) ||
      w.ipAddress.includes(search) ||
      w.id.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = filterStatus === 'ALL' || w.state === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const counts = ALL_STATUSES.reduce((acc, s) => {
    acc[s] = workers.filter(w => w.state === s).length;
    return acc;
  }, {} as Record<WorkerStatus, number>);

  return (
    <div className="p-6 space-y-5 animate-fade-in">
      {isError && (
        <ErrorBanner message="Failed to load workers from the coordinator." onRetry={() => refetch()} />
      )}

      {/* Stats strip */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'Total Workers', value: workers.length,                          color: 'text-slate-200' },
          { label: 'Online',        value: (counts.ONLINE ?? 0) + (counts.BUSY ?? 0), color: 'text-green-400' },
          { label: 'Busy',          value: counts.BUSY ?? 0,                        color: 'text-amber-400' },
          { label: 'Offline',       value: (counts.OFFLINE ?? 0) + (counts.UNHEALTHY ?? 0), color: 'text-slate-500' },
        ].map(s => (
          <div key={s.label} className="card p-4">
            <p className="section-title mb-1">{s.label}</p>
            <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-48">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text" placeholder="Search hostname, IP, or ID…"
            value={search} onChange={e => setSearch(e.target.value)}
            className="input-field pl-9"
          />
        </div>
        <div className="flex items-center gap-1.5">
          <Filter size={13} className="text-slate-500" />
          <select value={filterStatus} onChange={e => setFilterStatus(e.target.value as WorkerStatus | 'ALL')} className="select-field w-36">
            <option value="ALL">All Statuses</option>
            {ALL_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
        <div className="flex items-center bg-surface-800 border border-slate-700 rounded-lg p-1 gap-1">
          <button onClick={() => setViewMode('grid')}
            className={`p-1.5 rounded transition-colors ${viewMode === 'grid' ? 'bg-surface-600 text-slate-200' : 'text-slate-500 hover:text-slate-300'}`}>
            <LayoutGrid size={15} />
          </button>
          <button onClick={() => setViewMode('list')}
            className={`p-1.5 rounded transition-colors ${viewMode === 'list' ? 'bg-surface-600 text-slate-200' : 'text-slate-500 hover:text-slate-300'}`}>
            <List size={15} />
          </button>
        </div>
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="card p-6"><LoadingSkeleton rows={3} /></div>
      ) : filtered.length === 0 ? (
        <div className="card p-16 text-center">
          <p className="text-slate-500 text-sm">{workers.length === 0 ? 'No workers connected.' : 'No workers match your search.'}</p>
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(w => <WorkerCard key={w.id} worker={w} onClick={setSelectedWorker} />)}
        </div>
      ) : (
        <div className="card">
          <WorkerTable workers={filtered} onWorkerClick={setSelectedWorker} />
        </div>
      )}

      <WorkerDetailModal worker={selectedWorker} onClose={() => setSelectedWorker(null)} />
    </div>
  );
}
