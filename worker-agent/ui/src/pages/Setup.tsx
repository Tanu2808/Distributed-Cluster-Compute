import { useState } from 'react';

export default function Setup() {
  const [url, setUrl] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    if (!url) {
      setError('Coordinator URL is required');
      return;
    }
    if (!apiKey) {
      setError('API Key is required');
      return;
    }

    setLoading(true);

    // Simulate API call for Phase 1
    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
      // NOTE: Actual configuration persistence and connection logic will be implemented in Phase 2
    }, 1000);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold">Worker Setup</h1>
        <p className="text-worker-muted mt-1">Configure this worker to join a cluster</p>
        <div className="mt-4 p-3 bg-blue-500/10 border border-blue-500/30 rounded text-sm text-blue-400">
          Note: This is a temporary configuration interface for Phase 1. Submitting this form will validate inputs but will not persist the connection to the Coordinator yet.
        </div>
      </div>
      
      <form onSubmit={handleSubmit} className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-6">
        {error && (
          <div className="p-3 bg-red-500/10 border border-red-500/30 rounded text-sm text-red-400">
            {error}
          </div>
        )}
        {success && (
          <div className="p-3 bg-green-500/10 border border-green-500/30 rounded text-sm text-green-400">
            Configuration validated successfully! (Phase 2 will implement actual connection)
          </div>
        )}
        
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-worker-muted mb-1">Coordinator URL</label>
            <input 
              type="text" 
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" 
              placeholder="http://localhost:8080" 
              disabled={loading}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-worker-muted mb-1">API Key</label>
            <input 
              type="password" 
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" 
              placeholder="Enter coordinator API key" 
              disabled={loading}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-worker-muted mb-1">Worker Name (Optional)</label>
            <input 
              type="text" 
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" 
              placeholder="worker-node-1" 
              disabled={loading}
            />
          </div>
        </div>
        
        <div className="flex justify-end pt-4 border-t border-worker-border">
          <button 
            type="submit" 
            disabled={loading}
            className={`px-4 py-2 rounded font-medium transition-colors ${
              loading 
                ? 'bg-worker-primary/50 text-white/70 cursor-not-allowed'
                : 'bg-worker-primary hover:bg-worker-primaryHover text-white'
            }`}
          >
            {loading ? 'Connecting...' : 'Connect to Cluster'}
          </button>
        </div>
      </form>
    </div>
  )
}
