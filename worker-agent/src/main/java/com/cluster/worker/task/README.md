# Worker Task Management

## Purpose
The `task` package manages the local lifecycle of distributed tasks assigned to the Worker Agent. It translates incoming STOMP task requests into executing local threads and manages their state transitions.

## Responsibilities
- Validating whether the Worker actually has the required CPU/RAM to accept an assigned task via the `ResourceAdmissionService`.
- Mapping task types to their specific execution logic using the `TaskHandlerRegistry`.
- Tracking local state transitions (`PENDING` → `RUNNING` → `COMPLETED`/`FAILED`).
- Handling remote cancellation requests.

## Architecture / Flow
When a `TASK_ASSIGN` payload arrives, it is parsed into a `WorkerTask` entity. The `TaskHandlerRegistry` locates the correct `TaskHandler` (e.g., `SumRangeTaskHandler`). The handler claims resources, invokes the `execution` layer, and upon completion, sends a `TASK_STATUS` payload back to the Coordinator containing the serialized result.

## Key Files

| File | Responsibility |
|------|----------------|
| `WorkerTask.java` | Local state tracking entity representing a specific workload partition. |
| `TaskHandlerRegistry.java` | Dynamic dispatch registry mapping a `taskType` string to a concrete handler implementation. |
| `TaskHandler.java` | Strategy interface for validating and dispatching specific task formats. |
| `ComputeTaskHandler.java` / `SumRangeTaskHandler.java` | Concrete handlers parsing input JSON and invoking the execution engine. |
| `ResourceAdmissionService.java` | Validates local CPU/RAM constraints before allowing a task to transition to `RUNNING`. |

## Integration
This package acts as the middleman between the `communication` layer (receiving commands) and the `execution` layer (running the code). It utilizes `monitoring` data to enforce resource admission.
