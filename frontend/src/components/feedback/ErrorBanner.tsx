import { useState } from 'react';
import { AlertTriangle, X, RefreshCw } from 'lucide-react';

interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
}

/**
 * Dismissible error banner shown at the top of a page when an API call fails.
 * Provides a retry button if onRetry is supplied.
 */
export function ErrorBanner({ message, onRetry }: ErrorBannerProps) {
  const [dismissed, setDismissed] = useState(false);
  if (dismissed) return null;

  return (
    <div className="flex items-start gap-3 px-4 py-3 rounded-xl bg-red-500/10 border border-red-500/20 text-red-400 animate-fade-in">
      <AlertTriangle size={16} className="flex-shrink-0 mt-0.5" />
      <p className="flex-1 text-sm leading-relaxed">{message}</p>
      <div className="flex items-center gap-2 flex-shrink-0">
        {onRetry && (
          <button
            onClick={onRetry}
            className="flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-lg bg-red-500/15 hover:bg-red-500/25 transition-colors"
          >
            <RefreshCw size={12} />
            Retry
          </button>
        )}
        <button
          onClick={() => setDismissed(true)}
          className="p-1 rounded hover:bg-red-500/20 transition-colors"
          aria-label="Dismiss"
        >
          <X size={14} />
        </button>
      </div>
    </div>
  );
}
