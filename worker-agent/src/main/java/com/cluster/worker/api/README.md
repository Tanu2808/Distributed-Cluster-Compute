# Worker Agent Local API

## Purpose
The `api` package defines the local REST endpoints exposed directly by the Worker Agent daemon. These endpoints are strictly for local diagnostics and configuration by the node owner, accessed primarily through the embedded Worker Agent Dashboard (`worker-agent/ui/`).

## Responsibilities
- Exposing local hardware metrics, system info, and STOMP connection status.
- Providing endpoints for local node setup (e.g., submitting join credentials).
- Supplying real-time task execution data for active jobs running on this node.

## Key Files

| File | Responsibility |
|------|----------------|
| `WorkerController.java` | Basic liveness and static hardware capability info. |
| `ClusterController.java` | Endpoints for checking Coordinator connection status and processing local cluster enrollment/join credentials. |
| `DashboardController.java` | Aggregates data specifically for rendering the local React UI dashboard views. |
| `TaskController.java` | Returns the state of any `WorkerTask` instances actively running on this node. |
| `SettingsController.java` | Modifies local Worker Agent properties. |

## Integration
These controllers interact with the local `SystemMetricsProvider`, `WebSocketConnectionManager`, and `WorkerStateManager`. They do not communicate with the Coordinator over REST; all data returned by this API is strictly local.
