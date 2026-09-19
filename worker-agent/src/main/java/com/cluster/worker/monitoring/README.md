# Worker Hardware Monitoring

## Purpose
The `monitoring` package bridges the gap between the Java runtime and the host operating system. It relies heavily on the **OSHI (Operating System and Hardware Information)** library to gather accurate physical metrics regarding the node's capabilities.

## Responsibilities
- Identifying physical limits at startup (Total CPU cores, Max RAM).
- Polling fluctuating real-time metrics (CPU %, Free RAM, Disk Space).
- Normalizing OS-level data into the standard `SystemMetrics` domain model.

## Key Files

| File | Responsibility |
|------|----------------|
| `SystemMetricsProvider.java` | The primary interface for requesting hardware stats. |
| `OshiSystemMetricsProvider.java` | Concrete implementation combining CPU, Memory, Disk, and Network metrics via OSHI. |
| `CpuMetricsProvider.java` / `OshiCpuMetricsProvider.java` | Specific abstraction for querying CPU ticks and load averages. |
| `MemoryMetricsProvider.java` / `OshiMemoryMetricsProvider.java` | Specific abstraction for querying global physical memory limits and availability. |
| `SystemInfoConfig.java` | Spring configuration initializing the OSHI singleton. |

## Integration
Metrics collected by this package are heavily utilized by the `WorkerLifecycleService` during initial registration (to advertise capabilities) and by the `HeartbeatService` (to broadcast real-time availability to the Coordinator).
