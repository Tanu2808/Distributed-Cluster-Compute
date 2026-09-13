export default function Home() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Worker Overview</h1>
        <p className="text-worker-muted mt-1">Local metrics and operational status</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-2">CPU Usage</h3>
          <p className="text-2xl font-semibold">-- %</p>
        </div>
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-2">Memory Usage</h3>
          <p className="text-2xl font-semibold">-- / -- GB</p>
        </div>
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-2">Active Tasks</h3>
          <p className="text-2xl font-semibold">0</p>
        </div>
      </div>
    </div>
  )
}
