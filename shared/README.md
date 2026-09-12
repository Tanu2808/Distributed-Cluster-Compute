# Shared Protocol Module

The Shared Module is a lightweight Java library that contains all cross-cutting domain models, message envelopes, and protocol enumerations. 

This module ensures that both the **Coordinator** and **Worker Agents** share the exact same structural representation of data, achieving strict type safety over network boundaries.

## What is Built Till Now

* **`MessageEnvelope<T>`**: A generic envelope structure that standardizes how all WebSocket messages are formatted, enforcing a uniform JSON representation. Includes fields for `type`, `workerId`, `timestamp`, and the `payload`.
* **`MessageType` Enum**: Defines all possible operations on the cluster communication channels (`REGISTER`, `HEARTBEAT`, `RESOURCE_UPDATE`, `TASK_ASSIGN`, `TASK_STATUS`, etc.).
* **Connection Lifecycle Payloads**:
  * `RegisterMessage`: Contains static Hostname, OS version, architecture.
  * `HeartbeatMessage`: Liveness payload.
  * `ResourceUpdateMessage`: Used for periodic metrics updates of fluctuating stats (CPU %, free RAM, GPU state).
* **Task Protocol Models (Pre-built)**:
  * `TaskAssignmentMessage`: Definition structure for sending tasks to workers.
  * `TaskStatusMessage`: Definition structure for receiving status updates from workers.
  * `TaskResultMessage`: Definition for storing the output result payload from a task execution.
  * `TaskCancellationMessage`: Instruction payload used to abort a running task remotely.

## Capabilities

* Guarantees that parsing and serializing STOMP WebSocket messages never throw `NullPointerExceptions` due to mismatched properties or spelling errors in JSON structures across boundaries.
* Designed to be packaged as a lightweight `.jar` and imported directly into the `pom.xml` of any new microservice or agent added to the cluster ecosystem later.

## To Be Implemented

* Additional models will be added as new system features emerge, specifically around defining task dependencies (DAGs) and file-transfer protocol structures.
