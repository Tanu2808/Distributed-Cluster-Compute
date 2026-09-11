import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      /**
       * Data is considered fresh for 5 seconds.
       * Real-time freshness is handled by WebSocket cache invalidation.
       */
      staleTime: 5_000,

      /**
       * Keep cached data for 2 minutes after the last consumer unmounts.
       * Prevents re-fetching when navigating between pages quickly.
       */
      gcTime: 2 * 60 * 1000,

      /**
       * Retry failed requests up to 3 times with exponential backoff.
       * Backoff: 1s, 2s, 4s (capped at 30s).
       */
      retry: 3,
      retryDelay: (attempt) => Math.min(1000 * 2 ** attempt, 30_000),

      /**
       * Do not refetch on window focus — WebSocket handles live updates.
       */
      refetchOnWindowFocus: false,

      /**
       * Refetch when network reconnects (e.g. laptop wake from sleep).
       */
      refetchOnReconnect: true,
    },
    mutations: {
      retry: 1,
    },
  },
});
