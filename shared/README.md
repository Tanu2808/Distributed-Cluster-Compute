# Shared Protocol Module

## Purpose
The Shared Module is a lightweight Java library containing all cross-cutting domain models, message envelopes, and protocol enumerations. It acts as the definitive contract between the Coordinator and Worker Agents.

## Responsibilities
- **Protocol Compatibility**: Ensures that both sides of the network boundary use the exact same structural representation of data.
- **Type Safety**: Guarantees that STOMP WebSocket messages serialize and deserialize safely without `NullPointerException`s due to structural mismatch.

## Architecture / Flow
Both the `coordinator` and `worker-agent` declare the `shared` module as a Maven dependency. All STOMP traffic is sent inside a uniform JSON wrapper.

## Key Files

| File | Responsibility |
|------|----------------|
| `MessageEnvelope.java` | Standard wrapper containing `type`, `workerId`, `timestamp`, and the specific `payload`. |
| `MessageType.java` | Enum defining all STOMP operations (e.g., `REGISTER`, `HEARTBEAT`, `TASK_ASSIGN`). |
| `RegisterMessage.java` | Payload for initial node registration (hostname, OS). |
| `HeartbeatMessage.java` | Liveness payload sent periodically by workers. |
| `ResourceUpdateMessage` | Payload for reporting real-time CPU/RAM fluctuations. |
| `TaskAssignmentMessage` | Coordinator → Worker payload containing task inputs and resource constraints. |
| `TaskStatusMessage.java` | Worker → Coordinator payload tracking execution state (Running, Failed). |
| `TaskResultMessage.java` | Worker → Coordinator payload containing the final computation output. |
| `TaskCancellationMessage` | Instruction to abort a running task remotely. |

## Build Instructions
```bash
mvn clean install
```
