# Coordinator REST API Controllers

## Purpose
The `controller` package defines the external REST API boundary for the Coordinator. It handles HTTP requests from the Coordinator Dashboard (`frontend/`) and local worker agents, routing them to the underlying business logic in the `service` layer.

## Responsibilities
- Exposing endpoints for cluster metrics, worker enrollment, and job submission.
- Deserializing incoming JSON requests into DTOs.
- Returning standardized HTTP responses.
- Routing incoming STOMP messages mapped via `@MessageMapping` from connected workers.

## Key Files

| File | Responsibility |
|------|----------------|
| `ClusterController.java` | Handles cluster-wide operations like generating/rotating join codes and node enrollment. |
| `JobController.java` | Manages the submission and status querying of distributed compute Jobs and their partitions. |
| `WorkerController.java` | Exposes data regarding registered workers and their current physical capabilities. |
| `WorkerMessageController.java` | Processes incoming STOMP messages (e.g., `HEARTBEAT`, `RESOURCE_UPDATE`, `TASK_STATUS`) sent by Worker Agents. |
| `SettingsController.java` | Handles cluster configuration requests. |

## Integration
Controllers are the entry points. They rely entirely on the `service` package (e.g., `JobService`, `WorkerService`) to perform business logic and database transactions. STOMP controllers depend on the `websocket` package for routing.
