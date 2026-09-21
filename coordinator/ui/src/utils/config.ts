// ─── Environment Configuration ──────────────────────────────────────────────
// All env vars must be prefixed with VITE_ to be exposed to the browser bundle.

export const config = {
  /** When true, all API calls return mock data from src/mock/. No backend needed. */
  USE_MOCK_API: false,

  /** Base URL for the Spring Boot Coordinator REST API */
  API_BASE_URL:
    (import.meta.env.VITE_API_BASE_URL as string) || (import.meta.env.DEV ? "http://localhost:8080" : ""),

  /** WebSocket URL for real-time cluster events */
  WS_URL:
    (import.meta.env.VITE_WS_URL as string) || (import.meta.env.DEV ? "ws://localhost:8080/ws/cluster" : `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}/ws/cluster`),

  /**
   * Optional REST polling interval (ms) as fallback when WebSocket is unavailable.
   * Set to 0 to disable polling entirely. Defaults to 0 (disabled).
   */
  REST_POLL_INTERVAL_MS: parseInt(
    import.meta.env.VITE_REST_POLL_INTERVAL_MS || "0",
    10,
  ),

  /** API Username for Basic Auth */
  API_USERNAME: (import.meta.env.VITE_API_USERNAME as string) || "admin",

  /** API Password for Basic Auth */
  API_PASSWORD: (import.meta.env.VITE_API_PASSWORD as string) || "admin_secret",
} as const;
