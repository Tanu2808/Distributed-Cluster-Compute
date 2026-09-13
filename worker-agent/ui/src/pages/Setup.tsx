export default function Setup() {
  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Worker Setup</h1>
        <p className="text-worker-muted mt-1">Configure this worker to join a cluster</p>
      </div>
      
      <div className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-6">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-worker-muted mb-1">Coordinator URL</label>
            <input type="text" className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" placeholder="http://localhost:8080" />
          </div>
          <div>
            <label className="block text-sm font-medium text-worker-muted mb-1">API Key</label>
            <input type="password" className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" placeholder="Enter coordinator API key" />
          </div>
          <div>
            <label className="block text-sm font-medium text-worker-muted mb-1">Worker Name (Optional)</label>
            <input type="text" className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" placeholder="worker-node-1" />
          </div>
        </div>
        
        <div className="flex justify-end pt-4 border-t border-worker-border">
          <button className="bg-worker-primary hover:bg-worker-primaryHover text-white px-4 py-2 rounded font-medium transition-colors">
            Connect to Cluster
          </button>
        </div>
      </div>
    </div>
  )
}
