# Coordinator

The Coordinator is the central **control plane** (Master) of the Distributed Compute Cluster. It acts as the brain of the network, orchestrating workers and aggregating their capabilities into a logical cluster view.

## Responsibilities

* Worker registration and authentication.
* Tracking heartbeat events to monitor worker health and liveness.
* Managing real-time persistent connections with active Worker Agents.
* Aggregating resource metrics (CPU, RAM, GPU, Disk).
* Task and Job delegation (Planned).
* Providing REST and WebSocket APIs for the frontend UI.

## What is Built Till Now

* **STOMP WebSocket Server**: Fully integrated bidirectional WebSocket server over STOMP, mapped to `/ws/coordinator`.
* **Worker Message Controller**: Consumes typed events over STOMP for `REGISTER`, `HEARTBEAT`, and `RESOURCE_UPDATE`.
* **Security layer**: Basic Authentication configured directly into the WebSocket handshake process to validate connecting Worker Agents against an `api-key`.
* **Event Service**: Records incoming cluster events to a persistent database layer.
* **REST API Endpoints**: Exposes data required for the frontend Dashboard and Observability pages.

## Capabilities

* Capable of handling persistent STOMP sessions for multiple concurrent workers.
* Aggregates physical views (per-worker capabilities) into a logical cluster-wide resource pool view.
* Tracks time-based heartbeats to determine if a worker goes offline.

## To Be Implemented

* **Backend-to-Frontend Push Events**: The Coordinator requires a new STOMP/WebSocket endpoint (e.g., `/ws/cluster`) designed specifically for pushing live event feeds to the React frontend.
* **Task Scheduling Engine**: Implementing Directed Acyclic Graph (DAG) task execution logic and dispatching tasks over the `TASK_ASSIGN` protocol.
