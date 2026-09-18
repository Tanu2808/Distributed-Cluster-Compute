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

## Configuration

| Property | Environment Variable | Default | Description |
|---|---|---|---|
| `cluster.coordinator.advertised-url` | `COORDINATOR_ADVERTISED_URL` or `CLUSTER_COORDINATOR_ADVERTISED_URL` | `http://localhost:${server.port:8080}` | Reachable base URL of the Coordinator returned to Workers during enrollment. For LAN or remote deployments, set to the reachable IP or hostname (e.g. `http://192.168.137.165:8080`). |
| `cluster.security.api-username` | `CLUSTER_SECURITY_API_USERNAME` | `admin` | Username for basic authentication |
| `cluster.security.api-password` | `CLUSTER_SECURITY_API_PASSWORD` | `admin_secret` | Password for basic authentication |
| `cluster.heartbeat.timeout-seconds` | `CLUSTER_HEARTBEAT_TIMEOUT_SECONDS` | `30` | Heartbeat timeout threshold in seconds |

