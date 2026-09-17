import { useState, useEffect, useMemo } from 'react';
import { RefreshCw, Search, X, Copy, Check, Ban, Eye, ChevronDown, ChevronRight } from 'lucide-react';
import { useTasks } from '../hooks/useTasks';
import { taskApi } from '../services/taskApi';
import type { WorkerTask } from '../types';
import {
  MetricCard,
  StatusBadge,
  AlertBanner,
  ConfirmModal,
  Skeleton,
} from '../components';

type FilterTab = 'ALL' | 'ACTIVE' | 'COMPLETED' | 'FAILED';

function formatDuration(startedAt?: string, completedAt?: string | null): string {
  if (!startedAt) return '—';
  const start = new Date(startedAt).getTime();
  if (isNaN(start)) return '—';

  const end = completedAt ? new Date(completedAt).getTime() : Date.now();
  if (isNaN(end) || end < start) return '—';

  const diffMs = end - start;
  const totalSec = Math.floor(diffMs / 1000);
  const hrs = Math.floor(totalSec / 3600);
  const mins = Math.floor((totalSec % 3600) / 60);
  const secs = totalSec % 60;
  const pad = (n: number) => n.toString().padStart(2, '0');

  return `${pad(hrs)}:${pad(mins)}:${pad(secs)}`;
}


function formatTime(isoString?: string | null): string {
  if (!isoString) return '—';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  } catch {
    return '—';
  }
}

function formatMb(mb?: number): string {
  if (mb === undefined || mb === null || isNaN(mb) || mb < 0) return '—';
  if (mb >= 1024) return `${(mb / 1024).toFixed(1)} GB`;
  return `${mb} MB`;
}

function isCancellable(state: string): boolean {
  return ['QUEUED', 'RUNNING', 'VALIDATING', 'RECEIVED'].includes(state);
}

export default function Tasks() {
  const { tasks, loading, refreshing, error, refetch } = useTasks(4000);

  const [activeTab, setActiveTab] = useState<FilterTab>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);
  const [taskToCancel, setTaskToCancel] = useState<WorkerTask | null>(null);
  const [cancelLoading, setCancelLoading] = useState(false);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [copiedId, setCopiedId] = useState(false);
  const [inputExpanded, setInputExpanded] = useState(false);
  const [resultExpanded, setResultExpanded] = useState(false);

  // Live timer tick for running tasks
  const hasRunning = useMemo(() => tasks.some((t) => t.state === 'RUNNING'), [tasks]);
  const [, setTick] = useState(0);

  useEffect(() => {
    if (!hasRunning) return;
    const interval = setInterval(() => setTick((t) => t + 1), 1000);
    return () => clearInterval(interval);
  }, [hasRunning]);

  // Derived selected task from task list
  const selectedTask = useMemo(() => {
    if (!selectedTaskId) return null;
    return tasks.find((t) => t.taskId === selectedTaskId) || null;
  }, [tasks, selectedTaskId]);


  // Metric Summary Counts
  const counts = useMemo(() => {
    let running = 0;
    let queued = 0;
    let completed = 0;
    let failed = 0;

    for (const t of tasks) {
      if (t.state === 'RUNNING') running++;
      else if (t.state === 'QUEUED') queued++;
      else if (t.state === 'COMPLETED') completed++;
      else if (['FAILED', 'CANCELLED', 'REJECTED'].includes(t.state)) failed++;
    }

    return {
      total: tasks.length,
      running,
      queued,
      completed,
      failed,
      active: running + queued,
    };
  }, [tasks]);

  // Filtered Tasks
  const filteredTasks = useMemo(() => {
    return tasks.filter((task) => {
      // Tab filter
      if (activeTab === 'ACTIVE') {
        if (!['RUNNING', 'QUEUED', 'VALIDATING', 'RECEIVED'].includes(task.state)) return false;
      } else if (activeTab === 'COMPLETED') {
        if (task.state !== 'COMPLETED') return false;
      } else if (activeTab === 'FAILED') {
        if (!['FAILED', 'CANCELLED', 'REJECTED'].includes(task.state)) return false;
      }

      // Search filter
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesId = task.taskId?.toLowerCase().includes(q);
        const matchesType = task.taskType?.toLowerCase().includes(q);
        if (!matchesId && !matchesType) return false;
      }

      return true;
    });
  }, [tasks, activeTab, searchQuery]);

  const handleCopyId = (id: string) => {
    navigator.clipboard.writeText(id);
    setCopiedId(true);
    setTimeout(() => setCopiedId(false), 1800);
  };

  const handleConfirmCancel = async () => {
    if (!taskToCancel) return;
    setCancelLoading(true);
    try {
      await taskApi.cancelTask(taskToCancel.taskId);
      setNotification({
        type: 'success',
        message: `Task ${taskToCancel.taskId} cancelled successfully.`,
      });
      await refetch();
    } catch (err) {
      setNotification({
        type: 'error',
        message: err instanceof Error ? err.message : `Failed to cancel task ${taskToCancel.taskId}`,
      });
    } finally {
      setCancelLoading(false);
      setTaskToCancel(null);
    }
  };

  return (
    <div className="space-y-4 max-w-7xl">
      {/* 1. Page Header */}
      <div className="flex items-center justify-between pb-2.5 border-b border-console-border">
        <div className="flex items-center gap-3">
          <h1 className="text-base font-semibold tracking-tight text-console-text">
            Tasks
          </h1>
          <span className="text-xs font-mono text-console-textDim">
            ({counts.total} total)
          </span>
        </div>

        <button
          type="button"
          onClick={() => refetch()}
          disabled={refreshing}
          className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors disabled:opacity-50"
          title="Refresh tasks"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-console-textDim ${refreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Notifications / Errors */}
      {notification && (
        <AlertBanner
          type={notification.type}
          message={notification.message}
          onClose={() => setNotification(null)}
        />
      )}

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

      {/* 2. Summary Metric Row */}
      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3">
        <MetricCard label="TOTAL" value={counts.total} />
        <MetricCard label="RUNNING" value={counts.running} status={counts.running > 0 ? 'normal' : undefined} />
        <MetricCard label="QUEUED" value={counts.queued} status={counts.queued > 0 ? 'warning' : undefined} />
        <MetricCard label="COMPLETED" value={counts.completed} />
        <MetricCard label="FAILED" value={counts.failed} status={counts.failed > 0 ? 'critical' : undefined} />
      </div>

      {/* 3. Filter Bar & Search */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-2 bg-console-subtle border border-console-border rounded-sm">
        {/* Filter Tabs */}
        <div className="flex items-center gap-1 overflow-x-auto">
          {(
            [
              { id: 'ALL', label: 'All', count: counts.total },
              { id: 'ACTIVE', label: 'Active', count: counts.active },
              { id: 'COMPLETED', label: 'Completed', count: counts.completed },
              { id: 'FAILED', label: 'Failed', count: counts.failed },
            ] as const
          ).map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`px-2.5 py-1 text-xs font-medium rounded-sm transition-colors shrink-0 flex items-center gap-1.5 ${
                activeTab === tab.id
                  ? 'bg-white text-console-accent border border-slate-300 shadow-sm font-semibold'
                  : 'text-console-textMuted hover:text-console-text hover:bg-slate-200/50 border border-transparent'
              }`}
            >
              <span>{tab.label}</span>
              <span className="text-[10px] font-mono opacity-70">({tab.count})</span>
            </button>
          ))}
        </div>

        {/* Search Input */}
        <div className="relative min-w-[200px] sm:w-64">
          <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-console-textDim pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by ID or type..."
            className="w-full pl-8 pr-7 py-1 text-xs bg-white border border-console-border rounded-sm text-console-text placeholder:text-console-textDim focus:border-console-accent focus:outline-none font-mono"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-2 top-1/2 -translate-y-1/2 text-console-textDim hover:text-console-text"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>

      {/* 4. Dense Task Table */}
      <div className="bg-white border border-console-border rounded-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="bg-console-subtle border-b border-console-border text-console-textMuted font-medium text-[11px] uppercase tracking-wider select-none">
                <th className="py-2.5 px-3">Task ID</th>
                <th className="py-2.5 px-3">Type</th>
                <th className="py-2.5 px-3">Status</th>
                <th className="py-2.5 px-3">CPU</th>
                <th className="py-2.5 px-3">Memory</th>
                <th className="py-2.5 px-3">Started</th>
                <th className="py-2.5 px-3">Duration</th>
                <th className="py-2.5 px-3 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-console-borderSubtle">
              {loading && tasks.length === 0 ? (
                // Table Loading Skeletons
                Array.from({ length: 4 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="py-3 px-3"><Skeleton className="h-4 w-24" /></td>
                    <td className="py-3 px-3"><Skeleton className="h-4 w-20" /></td>
                    <td className="py-3 px-3"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-3"><Skeleton className="h-4 w-14" /></td>
                    <td className="py-3 px-3"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-3"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-3"><Skeleton className="h-4 w-16" /></td>
                    <td className="py-3 px-3 text-right"><Skeleton className="h-4 w-12 ml-auto" /></td>
                  </tr>
                ))
              ) : filteredTasks.length === 0 ? (
                // Empty States
                <tr>
                  <td colSpan={8} className="py-12 text-center">
                    {tasks.length === 0 ? (
                      <div className="space-y-1">
                        <div className="text-sm font-semibold text-console-text">No tasks</div>
                        <div className="text-xs text-console-textDim">
                          Tasks executed by this worker will appear here.
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-1">
                        <div className="text-sm font-semibold text-console-text">No matching tasks</div>
                        <div className="text-xs text-console-textDim">
                          Adjust filter or search criteria.
                        </div>
                      </div>
                    )}
                  </td>
                </tr>
              ) : (
                filteredTasks.map((task) => {
                  const isRunning = task.state === 'RUNNING';
                  const durationStr = isRunning
                    ? formatDuration(task.startedAt, null)
                    : formatDuration(task.startedAt, task.completedAt);
                  const cancellable = isCancellable(task.state);

                  return (
                    <tr
                      key={task.taskId}
                      onClick={() => setSelectedTaskId(task.taskId)}
                      className={`hover:bg-slate-50/80 transition-colors cursor-pointer ${
                        selectedTaskId === task.taskId ? 'bg-blue-50/60' : ''
                      }`}
                    >
                      {/* Task ID */}
                      <td className="py-2.5 px-3 font-mono font-medium text-console-text">
                        <span className="hover:underline">{task.taskId}</span>
                      </td>

                      {/* Type */}
                      <td className="py-2.5 px-3 font-mono text-console-textMuted">
                        {task.taskType || 'UNKNOWN'}
                      </td>

                      {/* Status */}
                      <td className="py-2.5 px-3">
                        <StatusBadge status={task.state} />
                      </td>

                      {/* CPU */}
                      <td className="py-2.5 px-3 font-mono text-console-textDim">
                        {task.requiredCpuCores ? `${task.requiredCpuCores} core${task.requiredCpuCores > 1 ? 's' : ''}` : '—'}
                      </td>

                      {/* Memory */}
                      <td className="py-2.5 px-3 font-mono text-console-textDim">
                        {formatMb(task.requiredMemoryMb)}
                      </td>

                      {/* Started */}
                      <td className="py-2.5 px-3 font-mono text-console-textDim">
                        {formatTime(task.startedAt)}
                      </td>

                      {/* Duration */}
                      <td className="py-2.5 px-3 font-mono text-console-text">
                        {durationStr}
                      </td>

                      {/* Actions */}
                      <td className="py-2.5 px-3 text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="inline-flex items-center gap-1.5 justify-end">
                          <button
                            type="button"
                            onClick={() => setSelectedTaskId(task.taskId)}
                            className="p-1 text-console-textDim hover:text-console-text rounded-sm hover:bg-slate-100 transition-colors"
                            title="View details"
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </button>

                          {cancellable && (
                            <button
                              type="button"
                              onClick={() => setTaskToCancel(task)}
                              className="p-1 text-rose-600 hover:text-rose-700 rounded-sm hover:bg-rose-50 transition-colors"
                              title="Cancel task"
                            >
                              <Ban className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 5. Right-Side Task Inspector Drawer */}
      {selectedTask && (
        <div className="fixed inset-0 z-40 flex justify-end bg-slate-900/30 backdrop-blur-[1px]">
          <div
            className="fixed inset-0"
            onClick={() => setSelectedTaskId(null)}
            aria-hidden="true"
          />

          <div className="relative w-full md:w-[460px] bg-white border-l border-console-border shadow-2xl flex flex-col h-full z-50 overflow-hidden">
            {/* Drawer Header */}
            <div className="px-4 py-3 border-b border-console-border bg-console-subtle flex items-center justify-between shrink-0">
              <div className="flex items-center gap-2.5">
                <h2 className="text-xs font-semibold uppercase tracking-wider text-console-text">
                  Task Details
                </h2>
                <StatusBadge status={selectedTask.state} />
              </div>
              <button
                type="button"
                onClick={() => setSelectedTaskId(null)}
                className="text-console-textDim hover:text-console-text p-1 rounded-sm"
                title="Close drawer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Drawer Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4 text-xs font-sans">
              {/* Task Section */}
              <div className="space-y-2 border-b border-console-borderSubtle pb-3">
                <span className="text-[10px] font-semibold uppercase tracking-wider text-console-textDim font-mono">
                  Task
                </span>
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Task ID</span>
                    <button
                      type="button"
                      onClick={() => handleCopyId(selectedTask.taskId)}
                      className="inline-flex items-center gap-1.5 font-mono text-[11px] text-console-text hover:text-console-accent transition-colors"
                      title="Click to copy ID"
                    >
                      <span>{selectedTask.taskId}</span>
                      {copiedId ? (
                        <Check className="w-3 h-3 text-emerald-600" />
                      ) : (
                        <Copy className="w-3 h-3 text-console-textDim" />
                      )}
                    </button>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Type</span>
                    <span className="font-mono text-console-text">{selectedTask.taskType}</span>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Status</span>
                    <StatusBadge status={selectedTask.state} />
                  </div>
                </div>
              </div>

              {/* Execution Section */}
              <div className="space-y-2 border-b border-console-borderSubtle pb-3">
                <span className="text-[10px] font-semibold uppercase tracking-wider text-console-textDim font-mono">
                  Execution
                </span>
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Received</span>
                    <span className="font-mono text-console-text">{formatTime(selectedTask.receivedAt)}</span>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Started</span>
                    <span className="font-mono text-console-text">{formatTime(selectedTask.startedAt)}</span>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Completed</span>
                    <span className="font-mono text-console-text">{formatTime(selectedTask.completedAt)}</span>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Duration</span>
                    <span className="font-mono text-console-text">
                      {selectedTask.state === 'RUNNING'
                        ? formatDuration(selectedTask.startedAt, null)
                        : formatDuration(selectedTask.startedAt, selectedTask.completedAt)}
                    </span>
                  </div>
                </div>
              </div>

              {/* Resources Section */}
              <div className="space-y-2 border-b border-console-borderSubtle pb-3">
                <span className="text-[10px] font-semibold uppercase tracking-wider text-console-textDim font-mono">
                  Resources
                </span>
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">CPU Cores</span>
                    <span className="font-mono text-console-text">
                      {selectedTask.requiredCpuCores ? `${selectedTask.requiredCpuCores} cores` : '—'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Memory</span>
                    <span className="font-mono text-console-text">{formatMb(selectedTask.requiredMemoryMb)}</span>
                  </div>
                  <div className="flex items-center justify-between py-1">
                    <span className="text-console-textDim">Timeout</span>
                    <span className="font-mono text-console-text">
                      {selectedTask.timeoutSeconds ? `${selectedTask.timeoutSeconds}s` : '—'}
                    </span>
                  </div>
                </div>
              </div>

              {/* Error Box (Only when error available) */}
              {selectedTask.errorMessage && (
                <div className="space-y-1.5">
                  <span className="text-[10px] font-semibold uppercase tracking-wider text-rose-700 font-mono">
                    Error
                  </span>
                  <div className="p-2.5 bg-rose-50 border border-rose-200 rounded-sm text-rose-800 font-mono text-[11px] break-words">
                    {selectedTask.errorMessage}
                  </div>
                </div>
              )}

              {/* Technical Input Section */}
              {selectedTask.input && Object.keys(selectedTask.input).length > 0 && (
                <div className="space-y-1.5">
                  <button
                    type="button"
                    onClick={() => setInputExpanded(!inputExpanded)}
                    className="flex items-center justify-between w-full py-1 text-left group"
                  >
                    <span className="text-[10px] font-semibold uppercase tracking-wider text-console-textDim font-mono group-hover:text-console-text">
                      Input Payload
                    </span>
                    {inputExpanded ? (
                      <ChevronDown className="w-3.5 h-3.5 text-console-textDim" />
                    ) : (
                      <ChevronRight className="w-3.5 h-3.5 text-console-textDim" />
                    )}
                  </button>

                  {inputExpanded && (
                    <pre className="p-2.5 bg-console-subtle border border-console-border rounded-sm font-mono text-[11px] text-console-text overflow-x-auto max-h-48">
                      {JSON.stringify(selectedTask.input, null, 2)}
                    </pre>
                  )}
                </div>
              )}

              {/* Result Section */}
              {selectedTask.result !== undefined && selectedTask.result !== null && (
                <div className="space-y-1.5">
                  <button
                    type="button"
                    onClick={() => setResultExpanded(!resultExpanded)}
                    className="flex items-center justify-between w-full py-1 text-left group"
                  >
                    <span className="text-[10px] font-semibold uppercase tracking-wider text-console-textDim font-mono group-hover:text-console-text">
                      Execution Result
                    </span>
                    {resultExpanded ? (
                      <ChevronDown className="w-3.5 h-3.5 text-console-textDim" />
                    ) : (
                      <ChevronRight className="w-3.5 h-3.5 text-console-textDim" />
                    )}
                  </button>

                  {resultExpanded && (
                    <pre className="p-2.5 bg-console-subtle border border-console-border rounded-sm font-mono text-[11px] text-emerald-800 overflow-x-auto max-h-48">
                      {typeof selectedTask.result === 'object'
                        ? JSON.stringify(selectedTask.result, null, 2)
                        : String(selectedTask.result)}
                    </pre>
                  )}
                </div>
              )}
            </div>

            {/* Drawer Footer Actions */}
            <div className="p-3 border-t border-console-border bg-console-subtle flex items-center justify-between shrink-0">
              {isCancellable(selectedTask.state) ? (
                <button
                  type="button"
                  onClick={() => setTaskToCancel(selectedTask)}
                  className="px-3 py-1.5 text-xs font-medium text-rose-700 hover:bg-rose-50 bg-white border border-rose-300 rounded-sm transition-colors inline-flex items-center gap-1.5"
                >
                  <Ban className="w-3.5 h-3.5" />
                  <span>Cancel Task</span>
                </button>
              ) : (
                <div />
              )}

              <button
                type="button"
                onClick={() => setSelectedTaskId(null)}
                className="px-3 py-1.5 text-xs font-medium text-console-text hover:bg-slate-50 bg-white border border-console-border hover:border-slate-400 rounded-sm transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 6. Cancel Confirmation Modal */}
      <ConfirmModal
        isOpen={Boolean(taskToCancel)}
        title="Cancel Task"
        message={`Are you sure you want to cancel task "${taskToCancel?.taskId}"? The execution will be terminated.`}
        confirmLabel={cancelLoading ? 'Cancelling...' : 'Cancel Task'}
        variant="danger"
        onConfirm={handleConfirmCancel}
        onCancel={() => setTaskToCancel(null)}
      />
    </div>
  );
}
