import { useDashboardHome } from '../hooks/useDashboardHome';

export default function Home() {
  const { data, loading, error } = useDashboardHome(3000);

  const formatMb = (mb: number) => {
    if (mb > 1024) return (mb / 1024).toFixed(1) + ' GB';
    return mb + ' MB';
  };

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold">Worker Dashboard</h1>
        <p className="text-worker-muted mt-1">Local metrics and operational status</p>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-lg">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-worker-card border border-worker-border rounded-lg p-4">
          <h3 className="text-worker-muted text-xs uppercase tracking-wider font-semibold mb-2">Worker Status</h3>
          <p className="text-xl font-semibold">
            {loading ? '...' : data?.workerStatus || 'UNKNOWN'}
          </p>
        </div>
        
        <div className="bg-worker-card border border-worker-border rounded-lg p-4">
          <h3 className="text-worker-muted text-xs uppercase tracking-wider font-semibold mb-2">Coordinator</h3>
          <p className="text-xl font-semibold">
            {loading ? '...' : data?.coordinatorConnection || 'UNKNOWN'}
          </p>
        </div>
        
        <div className="bg-worker-card border border-worker-border rounded-lg p-4">
          <h3 className="text-worker-muted text-xs uppercase tracking-wider font-semibold mb-2">Cluster</h3>
          <p className="text-xl font-semibold truncate" title={data?.clusterName}>
            {loading ? '...' : data?.clusterName || 'Not Configured'}
          </p>
        </div>
        
        <div className="bg-worker-card border border-worker-border rounded-lg p-4">
          <h3 className="text-worker-muted text-xs uppercase tracking-wider font-semibold mb-2">Hostname</h3>
          <p className="text-xl font-semibold truncate" title={data?.workerHostname}>
            {loading ? '...' : data?.workerHostname || 'Unknown'}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-4">CPU Usage</h3>
          <p className="text-3xl font-semibold mb-2">
            {loading ? '...' : `${(data?.cpuUsagePercent || 0).toFixed(1)}%`}
          </p>
          <p className="text-xs text-worker-muted">
            {data?.cpuCores} Cores Available
          </p>
        </div>
        
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-4">Memory Usage</h3>
          <p className="text-3xl font-semibold mb-2">
            {loading ? '...' : formatMb(data?.ramUsageMb || 0)}
          </p>
          <p className="text-xs text-worker-muted">
            of {formatMb(data?.ramTotalMb || 0)} Total
          </p>
        </div>
        
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-4">Task Execution</h3>
          <div className="flex justify-between items-end">
            <div>
              <p className="text-3xl font-semibold mb-2">
                {loading ? '...' : data?.activeTasks || 0}
              </p>
              <p className="text-xs text-worker-muted">Active</p>
            </div>
            <div className="text-right">
              <p className="text-xl font-semibold mb-2 text-worker-muted">
                {loading ? '...' : data?.queuedTasks || 0}
              </p>
              <p className="text-xs text-worker-muted">Queued</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
