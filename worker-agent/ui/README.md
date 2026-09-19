# Worker Agent UI

The Worker Agent UI is an embedded React/Vite dashboard hosted by the Worker Agent's Spring Boot server. It provides a local interface for diagnosing the worker's connection to the cluster and viewing hardware metrics locally.

## Key Architectural Files
- `src/services/apiClient.ts`: Core wrapper for communicating with the Worker Agent's local HTTP API endpoints (`/api/...`).
- `src/pages/Connection.tsx`: Dedicated page for displaying detailed STOMP connection diagnostics and latency.
- `src/hooks/useWorkerStatus.ts`: React hook abstracting the retrieval of real-time metrics for the node.
- `src/state/clusterStore.ts`: Global state for local node capabilities.

## Build Process
The UI is automatically built via the `frontend-maven-plugin` during the Worker Agent's Maven build phase. The Vite output is copied into `src/main/resources/static` and served directly by Spring Boot.
