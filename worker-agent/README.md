# Worker Agent

The Worker Agent is a lightweight daemon that runs on participating compute nodes (Slaves) within the Distributed Compute Cluster. It supplies computational resources to the cluster and awaits execution instructions from the Coordinator.

## Responsibilities

* Connecting to the Coordinator.
* Identifying and registering the machine.
* Reporting exact hardware capabilities (CPUs, GPUs, RAM, Disk limits).
* Periodically sending active heartbeats.
* Receiving tasks to execute (Planned).
* Returning computation results (Planned).

## What is Built Till Now

* **OSHI Hardware Monitoring**: Built-in integrations using OSHI to actively read System Metrics (CPU %, Memory limits/usage, Storage capacity/availability, and Network I/O).
* **WebSocket Connection Manager**: Capable of creating a persistent WebSocket connection to the Coordinator. It manages STOMP sessions and intercepts connectivity drops.
* **Resilient Reconnection Protocol**: Implements an exponential backoff retry mechanism (retrying starting at 5s up to max limits) if the connection to the Coordinator is lost.
* **Registration & Heartbeat Cycles**: Lifecycle Services manage registering the worker ID, handling `REGISTER_ACK` STOMP events, and starting automated scheduled Heartbeat/Resource Update broadcast intervals over STOMP.

## Capabilities

* **State Tracking**: Safely tracks its connection state (`STARTING`, `REGISTERING`, `ONLINE`, `DISCONNECTED`, `STOPPING`) locally.
* **Basic Auth Verification**: Configures and passes the required Basic `Authorization` header (`api-key`) inside WebSocket HTTP Handshakes seamlessly.
* Operates completely asynchronously to reduce overhead on the host machine.

## To Be Implemented

* **Task Execution Engine**: Parsing incoming `TASK_ASSIGN` protocol messages and executing the task payload (e.g. running an arbitrary shell command, a container, or a python script).
* **Task Lifecycle Management**: Implementing the logic to emit `TASK_STATUS` updates (Running, Failed, Completed) back to the Coordinator, along with the actual `TASK_RESULT`.
