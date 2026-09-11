import { useState } from 'react';
import { LayoutGrid, List, Search, Filter } from 'lucide-react';
import { WorkerCard } from './WorkerCard';
import { WorkerTable } from '../../components/tables/WorkerTable';
import { WorkerDetailModal } from '../../components/modals/WorkerDetailModal';
import { mockWorkers } from '../../mock/workers';
import type { Worker, WorkerStatus } from '../../types';

const ALL_STATUSES: WorkerStatus[] = ['ONLINE', 'BUSY', 'OFFLINE', 'UNHEALTHY', 'REGISTERING'];

export default function Nodes() {
  const [selectedWorker, setSelectedWorker] = useState<Worker | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid');
  const [search, setSearch] = useState('');
  const [filterStatus, setFilterStatus] = useState<WorkerStatus | 'ALL'>('ALL');

  const filtered = mockWorkers.filter(w => {
    const matchesSearch =
      w.hostname.toLowerCase().includes(search.toLowerCase()) ||
      w.ipAddress.includes(search) ||
      w.id.toLowerCase().includes(search.toLowerCase());
    const matchesStatus = filterStatus === 'ALL' || w.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  const counts = ALL_STATUSES.reduce((acc, s) => {
    acc[s] = mockWorkers.filter(w => w.status === s).length;
    return acc;
  }, {} as Record<WorkerStatus, number>);

  return (
    <div className="p-6 space-y-5 animate-fade-in">
      {/* Stats strip */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'Total Workers', value: mockWorkers.length, color: 'text-slate-200' },
          { label: 'Online',         value: counts.ONLINE + counts.BUSY, color: 'text-green-400' },
          { label: 'Busy',           value: counts.BUSY, color: 'text-amber-400' },
          { label: 'Offline',        value: counts.OFFLINE + counts.UNHEALTHY, color: 'text-slate-500' },
        ].map(s => (
          <div key={s.label} className="card p-4">
            <p className="section-title mb-1">{s.label}</p>
            <p className={`text-2xl font-bold ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Search */}
        <div className="relative flex-1 min-w-48">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            placeholder="Search hostname, IP, or ID…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="input-field pl-9"
          />
        </div>

        {/* Status filter */}
        <div className="flex items-center gap-1.5">
          <Filter size={13} className="text-slate-500" />
          <select
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value as WorkerStatus | 'ALL')}
            className="select-field w-36"
          >
            <option value="ALL">All Statuses</option>
            {ALL_STATUSES.map(s => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>

        {/* View toggle */}
        <div className="flex items-center bg-surface-800 border border-slate-700 rounded-lg p-1 gap-1">
          <button
            onClick={() => setViewMode('grid')}
            className={`p-1.5 rounded transition-colors ${viewMode === 'grid' ? 'bg-surface-600 text-slate-200' : 'text-slate-500 hover:text-slate-300'}`}
            title="Grid view"
          >
            <LayoutGrid size={15} />
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`p-1.5 rounded transition-colors ${viewMode === 'list' ? 'bg-surface-600 text-slate-200' : 'text-slate-500 hover:text-slate-300'}`}
            title="List view"
          >
            <List size={15} />
          </button>
        </div>
      </div>

      {/* Content */}
      {filtered.length === 0 ? (
        <div className="card p-16 text-center">
          <p className="text-slate-500 text-sm">No workers match your search.</p>
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filtered.map(w => (
            <WorkerCard key={w.id} worker={w} onClick={setSelectedWorker} />
          ))}
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
