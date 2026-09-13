import { useTasks } from '../hooks/useTasks';

export default function Home() {
  const { data, loading, error } = useTasks(3000);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Worker Overview</h1>
        <p className="text-worker-muted mt-1">Local metrics and operational status</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-worker-card border border-worker-border rounded-lg p-6 opacity-70">
          <h3 className="text-worker-muted text-sm font-medium mb-2">CPU Usage</h3>
          <p className="text-worker-muted text-sm mt-3">Not available</p>
        </div>
        <div className="bg-worker-card border border-worker-border rounded-lg p-6 opacity-70">
          <h3 className="text-worker-muted text-sm font-medium mb-2">Memory Usage</h3>
          <p className="text-worker-muted text-sm mt-3">Not available</p>
        </div>
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-2">Active Tasks</h3>
          <p className="text-2xl font-semibold">
            {loading ? '...' : error ? '0' : data?.tasks?.length ?? 0}
          </p>
        </div>
      </div>
    </div>
  )
}
