# Distributed Cluster Compute - Setup Guide

This guide provides the requirements and step-by-step instructions to run the Coordinator and Worker Agents on any compatible system.

## System Requirements

To build and run the project, your system needs the following installed:

- **Java Development Kit (JDK) 17** (or newer)
- **Maven 3.8+** (for building the project)
- **Node.js (v22+) & npm** (Optional, but recommended. The build process automatically downloads them locally via the `frontend-maven-plugin` if not present, but having them installed globally can speed up UI builds).

### Network Requirements
- **Coordinator**: Needs port `8080` open for the backend API/WebSocket and port `5173` (or built static files on 8080) for the UI.
- **Workers**: Must be able to reach the Coordinator's IP address and port `8080`.

---

## 1. Building the Project

The project is structured as a Maven multi-module project containing `shared`, `coordinator`, and `worker-agent`.

To build the entire project from the root directory:

```bash
# Navigate to the root directory
cd "Distributed Cluster Compute"

# Build all modules (skips tests for faster builds)
mvn clean package -DskipTests
```

This command will:
1. Compile the `shared` library.
2. Build the React UI in `coordinator/ui` and package it into the Spring Boot jar.
3. Build the `coordinator` Spring Boot application.
4. Build the `worker-agent` Spring Boot application.

---

## 2. Running the Coordinator

The Coordinator acts as the central control plane and serves the web UI.

**Option A: Using Maven (Development Mode)**
```bash
cd coordinator
mvn spring-boot:run
```

**Option B: Using the Packaged JAR (Production/Standalone Mode)**
```bash
cd coordinator/target
java -jar coordinator-0.0.1-SNAPSHOT.jar
```

Once running, the Coordinator will be available at:
- UI Dashboard: `http://localhost:8080` (or `http://localhost:5173` if running the Vite dev server separately).

### Changing the Coordinator Port
If port `8080` is already in use, you can easily change the port by passing the `--server.port` argument:
```bash
# Using Maven (Wrap the -D argument in quotes if using PowerShell on Windows)
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"

# Using the packaged JAR
java -jar coordinator-0.0.1-SNAPSHOT.jar --server.port=8082
```
*Note: If you change the Coordinator's port, remember to also update the Worker's configuration to connect to the new port (e.g., `--cluster.coordinator.url=http://<COORDINATOR_IP>:8082`).*

---

## 3. Running the Worker Agent (On Other Systems)

To run a worker on a different machine, you only need the packaged `worker-agent` JAR and Java 17 installed on that machine.

1. **Copy the JAR**: Transfer `worker-agent/target/worker-agent-0.0.1-SNAPSHOT.jar` to the target machine.
2. **Configure the Coordinator URL**: The worker needs to know where the Coordinator is located.

Run the worker and specify the Coordinator's URL via a command-line argument:

```bash
java -jar worker-agent-0.0.1-SNAPSHOT.jar --cluster.coordinator.url=http://<COORDINATOR_IP>:8080
```

*(Replace `<COORDINATOR_IP>` with the actual local network IP address of the machine running the Coordinator, e.g., `192.168.1.50`)*.

### Optional Worker Configuration
You can also set properties via environment variables or an `application.yml` file placed next to the JAR on the worker machine:

```bash
# Example using environment variables
export CLUSTER_COORDINATOR_URL=http://192.168.1.50:8080
java -jar worker-agent-0.0.1-SNAPSHOT.jar
```

---

## 4. Verification

1. Start the **Coordinator**.
2. Open the UI at `http://<COORDINATOR_IP>:8080`.
3. Go to the **Workers** page.
4. Start a **Worker Agent** on a different machine (or the same machine in a different terminal).
5. Within 5-10 seconds, the new worker should appear in the Coordinator's UI, and its resources (CPU, RAM) will be added to the Logical Cluster pool.
