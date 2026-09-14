import { useConnectionStatus } from '../hooks/useConnectionStatus'

export default function Connection() {
  const { connection, loading } = useConnectionStatus(3000);

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Connection Status</h1>
      <p className="text-worker-muted">Coordinator connection health and WebSocket status.</p>
      
      <div className="bg-worker-card border border-worker-border rounded-lg p-6 max-w-sm">
        <h3 className="text-worker-muted text-sm font-medium mb-2">WebSocket State</h3>
        <p className="text-xl font-semibold flex items-center">
          {loading ? 'Checking...' : (
             connection?.connected ? (
               <span className="text-worker-primary flex items-center"><span className="w-2 h-2 rounded-full bg-worker-primary mr-2"></span> Connected</span>
             ) : (
               <span className="text-red-500 flex items-center"><span className="w-2 h-2 rounded-full bg-red-500 mr-2"></span> Disconnected</span>
             )
          )}
        </p>
      </div>
    </div>
  )
}
