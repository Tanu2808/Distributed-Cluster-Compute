import { useState } from 'react';
import { clusterApi } from '../services/clusterApi';
import { useNavigate } from 'react-router-dom';

export default function Setup() {
  const [mode, setMode] = useState<'SELECT' | 'JOIN' | 'CREATE'>('SELECT');
  
  // Join state
  const [joinCode, setJoinCode] = useState('');
  
  // Create state
  const [clusterName, setClusterName] = useState('');
  const [isLocal, setIsLocal] = useState(true);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const navigate = useNavigate();

  const handleJoin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!joinCode) {
      setError('Join code is required');
      return;
    }

    setLoading(true);

    try {
      const response = await clusterApi.joinCluster(joinCode);
      if (response.status === 'SUCCESS') {
        setSuccess('Successfully joined cluster!');
        setTimeout(() => navigate('/home'), 1500);
      } else {
        setError(response.message || 'Failed to join cluster');
      }
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Failed to join cluster');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!clusterName) {
      setError('Cluster name is required');
      return;
    }

    setLoading(true);

    try {
      await clusterApi.createCluster(clusterName, isLocal);
      setSuccess('Successfully created cluster! Configuration saved.');
      setTimeout(() => navigate('/home'), 1500);
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Failed to create cluster');
    } finally {
      setLoading(false);
    }
  };

  if (mode === 'SELECT') {
    return (
      <div className="max-w-3xl mx-auto flex flex-col items-center justify-center min-h-[60vh] space-y-8">
        <div className="text-center space-y-2">
          <h1 className="text-4xl font-bold text-worker-text tracking-tight">CLUSTER COMPUTE</h1>
          <p className="text-worker-muted text-lg">Turn this computer into part of a distributed compute cluster.</p>
        </div>
        
        <div className="flex gap-6 mt-8">
          <button 
            onClick={() => setMode('CREATE')}
            className="px-8 py-4 bg-worker-card border border-worker-border hover:border-worker-primary hover:bg-worker-primary/5 rounded-xl flex flex-col items-center transition-all group"
          >
            <span className="text-xl font-semibold mb-2 group-hover:text-worker-primary">Create a Cluster</span>
            <span className="text-sm text-worker-muted text-center max-w-[200px]">Start a new cluster and make this node the first member.</span>
          </button>

          <button 
            onClick={() => setMode('JOIN')}
            className="px-8 py-4 bg-worker-card border border-worker-border hover:border-worker-primary hover:bg-worker-primary/5 rounded-xl flex flex-col items-center transition-all group"
          >
            <span className="text-xl font-semibold mb-2 group-hover:text-worker-primary">Join a Cluster</span>
            <span className="text-sm text-worker-muted text-center max-w-[200px]">Use a join code to add this node to an existing cluster.</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto space-y-8">
      <div>
        <button onClick={() => { setMode('SELECT'); setError(null); setSuccess(null); }} className="text-sm text-worker-primary hover:underline mb-4 flex items-center">
          ← Back to selection
        </button>
        <h1 className="text-2xl font-bold">{mode === 'JOIN' ? 'Join Cluster' : 'Create Cluster'}</h1>
        <p className="text-worker-muted mt-1">
          {mode === 'JOIN' ? 'Enter your 16-character cluster join code below.' : 'Configure your new distributed compute cluster.'}
        </p>
      </div>
      
      {mode === 'JOIN' ? (
        <form onSubmit={handleJoin} className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-6">
          {error && (
            <div className="p-3 bg-red-500/10 border border-red-500/30 rounded text-sm text-red-400">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 bg-green-500/10 border border-green-500/30 rounded text-sm text-green-400">
              {success}
            </div>
          )}
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-worker-muted mb-1">Cluster Code</label>
              <input 
                type="text" 
                value={joinCode}
                onChange={(e) => setJoinCode(e.target.value)}
                className="w-full bg-worker-bg border border-worker-border rounded p-3 text-lg font-mono focus:border-worker-primary focus:outline-none placeholder:opacity-50" 
                placeholder="XXXX-XXXX-XXXX-XXXX" 
                disabled={loading}
              />
            </div>
          </div>
          
          <div className="flex justify-end pt-4 border-t border-worker-border">
            <button 
              type="submit" 
              disabled={loading}
              className={`px-6 py-2 rounded font-medium transition-colors ${
                loading 
                  ? 'bg-worker-primary/50 text-white/70 cursor-not-allowed'
                  : 'bg-worker-primary hover:bg-worker-primaryHover text-white'
              }`}
            >
              {loading ? 'Connecting...' : 'Connect'}
            </button>
          </div>
        </form>
      ) : (
        <form onSubmit={handleCreate} className="bg-worker-card border border-worker-border rounded-lg p-6 space-y-6">
          {error && (
            <div className="p-3 bg-red-500/10 border border-red-500/30 rounded text-sm text-red-400">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 bg-green-500/10 border border-green-500/30 rounded text-sm text-green-400">
              {success}
            </div>
          )}
          
          <div className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-worker-muted mb-1">Cluster Name</label>
              <input 
                type="text" 
                value={clusterName}
                onChange={(e) => setClusterName(e.target.value)}
                className="w-full bg-worker-bg border border-worker-border rounded p-2 focus:border-worker-primary focus:outline-none" 
                placeholder="My Compute Cluster" 
                disabled={loading}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-worker-muted mb-3">Coordinator Selection</label>
              <div className="space-y-3">
                <label className="flex items-center space-x-3 p-3 border border-worker-border rounded hover:bg-worker-border/30 cursor-pointer">
                  <input 
                    type="radio" 
                    name="coordinator"
                    checked={isLocal} 
                    onChange={() => setIsLocal(true)}
                    className="text-worker-primary bg-worker-bg border-worker-border focus:ring-worker-primary focus:ring-offset-worker-bg"
                  />
                  <div>
                    <div className="font-medium text-worker-text text-sm">This computer</div>
                    <div className="text-xs text-worker-muted">Provision a new coordinator node locally.</div>
                  </div>
                </label>
                <label className="flex items-center space-x-3 p-3 border border-worker-border rounded hover:bg-worker-border/30 cursor-pointer">
                  <input 
                    type="radio" 
                    name="coordinator"
                    checked={!isLocal} 
                    onChange={() => setIsLocal(false)}
                    className="text-worker-primary bg-worker-bg border-worker-border focus:ring-worker-primary focus:ring-offset-worker-bg"
                  />
                  <div>
                    <div className="font-medium text-worker-text text-sm">Existing server</div>
                    <div className="text-xs text-worker-muted">Connect to an already running coordinator.</div>
                  </div>
                </label>
              </div>
            </div>
          </div>
          
          <div className="flex justify-end pt-4 border-t border-worker-border">
            <button 
              type="submit" 
              disabled={loading}
              className={`px-6 py-2 rounded font-medium transition-colors ${
                loading 
                  ? 'bg-worker-primary/50 text-white/70 cursor-not-allowed'
                  : 'bg-worker-primary hover:bg-worker-primaryHover text-white'
              }`}
            >
              {loading ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
