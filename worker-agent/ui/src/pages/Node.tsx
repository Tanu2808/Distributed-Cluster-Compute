import { useDashboardNode } from '../hooks/useDashboardNode';

export default function Node() {
  const { data, loading, error } = useDashboardNode(5000);

  const formatGb = (mb: number | undefined) => {
    if (!mb) return '0 GB';
    return (mb / 1024).toFixed(2) + ' GB';
  };

  const formatBytes = (bytes: number | undefined) => {
    if (!bytes) return '0 B';
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    if (bytes === 0) return '0 B';
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(2) + ' ' + sizes[i];
  };

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold">Node Hardware</h1>
        <p className="text-worker-muted mt-1">Detailed system information and hardware metrics.</p>
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-lg">
          {error}
        </div>
      )}

      {loading ? (
        <div className="text-worker-muted">Loading node information...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-worker-card border border-worker-border rounded-lg p-6">
            <h3 className="text-worker-muted text-sm font-medium mb-4 border-b border-worker-border pb-2">System</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-worker-muted">OS</span>
                <span className="font-semibold">{data?.osName} {data?.osVersion}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-worker-muted">Architecture</span>
                <span className="font-semibold">{data?.architecture}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-worker-muted">Agent Version</span>
                <span className="font-semibold">{data?.agentVersion}</span>
              </div>
            </div>
          </div>

          <div className="bg-worker-card border border-worker-border rounded-lg p-6">
            <h3 className="text-worker-muted text-sm font-medium mb-4 border-b border-worker-border pb-2">Processor</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-worker-muted">Model</span>
                <span className="font-semibold truncate ml-4" title={data?.cpuModel}>{data?.cpuModel}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-worker-muted">Usage</span>
                <span className="font-semibold">{data?.cpuUsagePercent.toFixed(1)}%</span>
              </div>
            </div>
          </div>

          <div className="bg-worker-card border border-worker-border rounded-lg p-6">
            <h3 className="text-worker-muted text-sm font-medium mb-4 border-b border-worker-border pb-2">Memory & Storage</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-worker-muted">RAM</span>
                <span className="font-semibold">{formatGb(data?.memoryUsedMb)} / {formatGb(data?.memoryTotalMb)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-worker-muted">Disk</span>
                <span className="font-semibold">{formatGb(data?.diskUsedMb)} / {formatGb(data?.diskTotalMb)}</span>
              </div>
            </div>
          </div>

          <div className="bg-worker-card border border-worker-border rounded-lg p-6">
            <h3 className="text-worker-muted text-sm font-medium mb-4 border-b border-worker-border pb-2">Network & GPU</h3>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-worker-muted">Bytes Sent</span>
                <span className="font-semibold">{formatBytes(data?.networkBytesSent)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-worker-muted">Bytes Received</span>
                <span className="font-semibold">{formatBytes(data?.networkBytesReceived)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-worker-muted">GPU Count</span>
                <span className="font-semibold">{data?.gpuCount}</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
