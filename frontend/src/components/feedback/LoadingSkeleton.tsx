interface LoadingSkeletonProps {
  rows?: number;
  className?: string;
}

/** Animated pulse skeleton for card/table loading states */
export function LoadingSkeleton({ rows = 4, className = '' }: LoadingSkeletonProps) {
  return (
    <div className={`space-y-3 ${className}`}>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="animate-pulse flex gap-3 items-center">
          <div className="w-8 h-8 rounded-lg bg-slate-800 flex-shrink-0" />
          <div className="flex-1 space-y-2">
            <div className="h-2.5 bg-slate-800 rounded-full w-3/4" />
            <div className="h-2 bg-slate-800/60 rounded-full w-1/2" />
          </div>
          <div className="h-5 w-16 bg-slate-800 rounded-full flex-shrink-0" />
        </div>
      ))}
    </div>
  );
}

/** Full-card skeleton for chart/resource cards */
export function CardSkeleton({ className = '' }: { className?: string }) {
  return (
    <div className={`card p-5 animate-pulse ${className}`}>
      <div className="flex justify-between items-start mb-4">
        <div className="space-y-2 flex-1">
          <div className="h-2 bg-slate-800 rounded-full w-1/3" />
          <div className="h-6 bg-slate-800 rounded-lg w-1/2" />
        </div>
        <div className="w-10 h-10 bg-slate-800 rounded-lg" />
      </div>
      <div className="h-20 bg-slate-800/50 rounded-lg" />
    </div>
  );
}
