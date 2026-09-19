# Coordinator Service Layer

## Purpose
The `service` package contains the core business logic and transactional boundaries for the Coordinator. It orchestrates the entire cluster lifecycle, from managing connected worker state to scheduling and executing distributed jobs.

## Responsibilities
- Tracking worker heartbeats and marking dead nodes offline.
- Creating Jobs and pushing them through their state machine (`SUBMITTED` → `PARTITIONING` → `QUEUED` → `RUNNING`).
- Matching unassigned Tasks to eligible Workers based on CPU and Memory constraints.
- Managing resource reservations to prevent over-provisioning worker nodes.

## Architecture / Flow
```text
[REST API] → JobService → (Creates Job & Tasks)
                             ↓
[Scheduled Polling] → SchedulerService → (Finds Eligible Worker)
                             ↓
                      ResourceReservationService → (Reserves CPU/RAM)
                             ↓
[STOMP WebSocket] ← (Sends TASK_ASSIGN message)
```

## Key Files

| File | Responsibility |
|------|----------------|
| `JobService.java` | Core transaction boundary for creating Jobs and tracking overall completion state. |
| `SchedulerService.java` | Periodically evaluates unassigned Tasks and dispatches them to capable `ONLINE` workers. |
| `TaskExecutionService.java` | Processes incoming `TASK_STATUS` updates and tracks individual Task lifecycle states. |
| `WorkerService.java` | Handles worker registration logic and synchronizes worker metrics. |
| `ResourceReservationService.java` | Prevents race conditions by reserving worker CPU/RAM before assigning tasks. |
| `HeartbeatService.java` | Sweeps for missed heartbeats and transitions stale workers to `OFFLINE`. |

## Integration
Services are invoked by the `controller` package. They rely on the `repository` package for persistence and the `partitioner` package to split incoming jobs. They push outward via the `websocket` package.
