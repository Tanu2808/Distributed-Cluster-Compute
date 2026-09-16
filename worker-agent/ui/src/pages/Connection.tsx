import { useConnectionStatus } from '../hooks/useConnectionStatus'

export default function Connection() {
  const { connection, loading } = useConnectionStatus(3000);

  const formatDate = (isoString: string | null | undefined) => {
    if (!isoString) return 'N/A';
    return new Date(isoString).toLocaleString();
  };

  return (
    <div className="space-y-6 max-w-3xl">
      <h1 className="text-2xl font-bold">Connection Status</h1>
      <p className="text-worker-muted">Coordinator connection health and WebSocket status.</p>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-4">WebSocket State</h3>
          <p className="text-xl font-semibold flex items-center mb-4">
            {loading ? 'Checking...' : (
               connection?.connectionState === 'ONLINE' ? (
                 <span className="text-worker-primary flex items-center"><span className="w-2 h-2 rounded-full bg-worker-primary mr-2"></span> Online</span>
               ) : connection?.connectionState === 'CONNECTING' || connection?.connectionState === 'REGISTERING' || connection?.connectionState === 'RECONNECTING' ? (
                 <span className="text-yellow-400 flex items-center"><span className="w-2 h-2 rounded-full bg-yellow-400 mr-2"></span> {connection.connectionState}</span>
               ) : (
                 <span className="text-red-500 flex items-center"><span className="w-2 h-2 rounded-full bg-red-500 mr-2"></span> {connection?.connectionState || 'Disconnected'}</span>
               )
            )}
          </p>

          <div className="space-y-3 text-sm mt-6 border-t border-worker-border pt-4">
            <div className="flex justify-between">
              <span className="text-worker-muted">Coordinator URL</span>
              <span className="font-mono truncate ml-4" title={connection?.coordinatorUrl}>{connection?.coordinatorUrl || 'N/A'}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-worker-muted">Connected Since</span>
              <span>{formatDate(connection?.connectedSince)}</span>
            </div>
          </div>
        </div>

        <div className="bg-worker-card border border-worker-border rounded-lg p-6">
          <h3 className="text-worker-muted text-sm font-medium mb-4">Diagnostics</h3>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between">
              <span className="text-worker-muted">Last Heartbeat</span>
              <span>{formatDate(connection?.lastSuccessfulHeartbeat)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-worker-muted">Last Message Tx</span>
              <span>{formatDate(connection?.lastMessageTimestamp)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-worker-muted">Reconnect Count</span>
              <span>{connection?.reconnectCount ?? 0}</span>
            </div>
          </div>

          {connection?.lastConnectionError && (
            <div className="mt-6 border-t border-worker-border pt-4">
              <h4 className="text-red-400 text-xs font-semibold mb-2">LAST ERROR</h4>
              <p className="text-xs font-mono text-red-300 break-words bg-red-900/20 p-2 rounded border border-red-500/20">
                {connection.lastConnectionError}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
