# Worker Agent Dashboard

## Purpose
The Worker Agent Dashboard is a localized React 18 UI embedded directly into each Worker Agent node. Unlike the cluster-wide frontend, this dashboard is designed strictly for local diagnostics, allowing a node administrator to view the agent's connection health, active task execution, and precise local hardware metrics.

## Architecture
- **Tech Stack**: React 18 + Vite + TailwindCSS
- **Backend Integration**: It communicates exclusively with the **local** Worker Agent's Spring Boot HTTP endpoints (e.g., `GET /api/worker/...`). It does not communicate directly with the Coordinator.
- **Maven Embedding**: During the Maven build phase (via `frontend-maven-plugin`), the Vite production output is built and copied into `src/main/resources/static`. This allows the Worker Agent daemon to serve its own UI on its standard port (default `8081`) without requiring a separate web server.

## Routing and Views
- `/` (Home): High-level overview of the local worker.
- `/connection` (Connection): Detailed diagnostics on STOMP connection state, retry backoff metrics, and ping latency to the Coordinator.
- `/node` (Node): Real-time view of local CPU, RAM, and disk utilization monitored by OSHI.
- `/tasks` (Tasks): Real-time view of Tasks currently executing on this specific worker.
- `/setup` (Setup): Configuration flow if the agent lacks registration credentials.
- `/settings` (Settings): Local node configuration values.

## State Management
Global UI state is managed via **Zustand** in `clusterStore.ts`, caching local worker capabilities. State fetching is abstracted behind custom React hooks (`useWorkerStatus`, `useTasks`).

## Key Files
| File | Responsibility |
|------|----------------|
| `src/services/apiClient.ts` | Core wrapper for communicating with the Worker Agent's local HTTP API endpoints (`/api/...`). |
| `src/hooks/useWorkerStatus.ts` | React hook abstracting the retrieval of real-time OSHI metrics for the node. |
| `src/hooks/useConnectionStatus.ts` | Hook for polling STOMP latency and backoff states from the local backend. |
| `src/state/clusterStore.ts` | Global state for local node capabilities. |

## Development
To run the UI in standalone development mode (assuming the local Worker Agent is running on port 8081):
```bash
npm install
npm run dev
```
