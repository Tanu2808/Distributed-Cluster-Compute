package com.cluster.worker.service;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.TaskAssignmentMessage;
import com.cluster.shared.protocol.TaskResultMessage;
import com.cluster.shared.protocol.TaskStatusMessage;
import com.cluster.worker.communication.WebSocketConnectionManager;
import com.cluster.worker.config.WorkerConfig;
import com.cluster.worker.model.ExecutionState;
import com.cluster.worker.monitoring.SystemMetricsProvider;
import com.cluster.worker.task.ResourceAdmissionService;
import com.cluster.worker.task.TaskHandler;
import com.cluster.worker.task.TaskHandlerRegistry;
import com.cluster.worker.task.TaskState;
import com.cluster.worker.task.WorkerTask;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final WorkerConfig config;
    private final WorkerStateManager stateManager;
    private final ResourceAdmissionService admissionService;
    private final SystemMetricsProvider metricsProvider;
    private final TaskHandlerRegistry handlerRegistry;
    private final WebSocketConnectionManager connectionManager;
    private final com.cluster.worker.persistence.WorkerConfigurationStore configStore;

    private final ConcurrentHashMap<String, WorkerTask> allTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Future<?>> runningFutures = new ConcurrentHashMap<>();
    
    private final BlockingQueue<WorkerTask> taskQueue;
    private final ExecutorService dispatchPool;
    private final ExecutorService taskExecutionPool;

    private final AtomicInteger dispatchThreadCounter = new AtomicInteger(0);
    private final AtomicInteger execThreadCounter = new AtomicInteger(0);

    public TaskService(WorkerConfig config,
                       WorkerStateManager stateManager,
                       ResourceAdmissionService admissionService,
                       SystemMetricsProvider metricsProvider,
                       TaskHandlerRegistry handlerRegistry,
                       WebSocketConnectionManager connectionManager,
                       com.cluster.worker.persistence.WorkerConfigurationStore configStore) {
        this.config = config;
        this.stateManager = stateManager;
        this.admissionService = admissionService;
        this.metricsProvider = metricsProvider;
        this.handlerRegistry = handlerRegistry;
        this.connectionManager = connectionManager;
        this.configStore = configStore;

        int maxConcurrent = Math.max(1, config.getExecution().getMaxConcurrentTasks());
        int queueCap = Math.max(1, config.getExecution().getQueueCapacity());
        this.taskQueue = new LinkedBlockingQueue<>(queueCap);
        
        // Dispatch threads to drain the taskQueue
        this.dispatchPool = Executors.newFixedThreadPool(maxConcurrent, r -> {
            Thread t = new Thread(r, "task-dispatch-" + dispatchThreadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });

        // Bounded execution pool to run handlers with timeout support
        this.taskExecutionPool = Executors.newFixedThreadPool(maxConcurrent, r -> {
            Thread t = new Thread(r, "task-exec-" + execThreadCounter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        
        // Start background dispatch threads
        for (int i = 0; i < maxConcurrent; i++) {
            this.dispatchPool.submit(this::processQueue);
        }
    }

    public List<WorkerTask> getActiveTasks() {
        return allTasks.values().stream()
                .filter(t -> t.getState() == TaskState.RUNNING || t.getState() == TaskState.VALIDATING)
                .toList();
    }

    public List<WorkerTask> getQueuedTasks() {
        return allTasks.values().stream()
                .filter(t -> t.getState() == TaskState.QUEUED)
                .toList();
    }
    
    public List<WorkerTask> getAllTasks() {
        return new ArrayList<>(allTasks.values());
    }

    public Optional<WorkerTask> getTask(String taskId) {
        if (taskId == null) return Optional.empty();
        return Optional.ofNullable(allTasks.get(taskId));
    }

    public void submitTask(TaskAssignmentMessage assignment) {
        if (assignment == null || assignment.getTaskId() == null || assignment.getTaskId().trim().isEmpty()) {
            log.warn("Rejected invalid task submission: missing or empty task ID");
            return;
        }

        String taskId = assignment.getTaskId().trim();
        log.info("Received TASK_ASSIGN for ID: {}", taskId);

        // Reject duplicate task IDs safely
        if (allTasks.containsKey(taskId)) {
            log.warn("Duplicate task ID rejected: {}", taskId);
            sendStatus(taskId, TaskState.REJECTED, "Duplicate task ID: " + taskId);
            return;
        }

        // Validate task assignment fields
        if (assignment.getTaskType() == null || assignment.getTaskType().trim().isEmpty()) {
            log.warn("Task {} rejected: missing task type", taskId);
            sendStatus(taskId, TaskState.REJECTED, "Missing task type");
            return;
        }

        if (assignment.getRequiredCpuCores() < 0 || assignment.getRequiredMemoryMb() < 0) {
            log.warn("Task {} rejected: negative resource requirements", taskId);
            sendStatus(taskId, TaskState.REJECTED, "Invalid negative resource requirements");
            return;
        }

        // Validate handler exists
        Optional<TaskHandler> handlerOpt = handlerRegistry.getHandler(assignment.getTaskType());
        if (handlerOpt.isEmpty()) {
            log.warn("Task {} rejected: unsupported task type {}", taskId, assignment.getTaskType());
            sendStatus(taskId, TaskState.REJECTED, "Unsupported task type: " + assignment.getTaskType());
            return;
        }

        WorkerTask task = new WorkerTask();
        task.setTaskId(taskId);
        task.setTaskType(assignment.getTaskType());
        task.setInput(assignment.getInput());
        task.setRequiredCpuCores(assignment.getRequiredCpuCores());
        task.setRequiredMemoryMb(assignment.getRequiredMemoryMb());
        task.setTimeoutSeconds(assignment.getTimeoutSeconds() > 0 ? 
                               assignment.getTimeoutSeconds() : 
                               config.getExecution().getDefaultTimeoutSeconds());
        task.setJobId(assignment.getJobId());
        task.setPartitionId(assignment.getPartitionId());
        task.setTotalPartitions(assignment.getTotalPartitions());
        task.setState(TaskState.VALIDATING);

        WorkerTask existing = allTasks.putIfAbsent(taskId, task);
        if (existing != null) {
            log.warn("Duplicate task ID rejected during concurrent insert: {}", taskId);
            sendStatus(taskId, TaskState.REJECTED, "Duplicate task ID: " + taskId);
            return;
        }

        // Admission control and queue check under lock
        synchronized (this) {
            if (taskQueue.remainingCapacity() == 0) {
                rejectTask(task, "Worker queue is full");
                return;
            }

            boolean reserved = admissionService.tryReserve(task, metricsProvider.collectMetrics(), config.getExecution());
            if (!reserved) {
                rejectTask(task, "Insufficient resources");
                return;
            }

            task.setState(TaskState.QUEUED);
            boolean queued = taskQueue.offer(task);
            if (!queued) {
                admissionService.release(task);
                rejectTask(task, "Worker queue is full");
                return;
            }

            sendStatus(task.getTaskId(), TaskState.QUEUED, "Task queued successfully");
            log.info("Task {} queued successfully", taskId);
        }
    }
    
    public boolean cancelTask(String taskId) {
        if (taskId == null) return false;
        WorkerTask task = allTasks.get(taskId);
        if (task == null) return false;
        
        synchronized (task) {
            TaskState current = task.getState();
            if (current.isTerminal()) {
                log.debug("Task {} is already in terminal state {}, cancel ignored", taskId, current);
                return false;
            }

            if (current == TaskState.QUEUED || current == TaskState.VALIDATING || current == TaskState.RECEIVED) {
                taskQueue.remove(task);
                task.setState(TaskState.CANCELLED);
                task.setErrorMessage("Task cancelled while in queue");
                task.setCompletedAt(Instant.now());
                admissionService.release(task);
                sendStatus(taskId, TaskState.CANCELLED, "Task cancelled while in queue");
                sendResult(task);
                updateExecutionState();
                log.info("Task {} cancelled while queued", taskId);
                return true;
            } else if (current == TaskState.RUNNING) {
                task.setState(TaskState.CANCELLED);
                task.setErrorMessage("Task cancelled during execution");
                task.setCompletedAt(Instant.now());

                Future<?> future = runningFutures.get(taskId);
                if (future != null) {
                    future.cancel(true);
                }
                admissionService.release(task);
                sendStatus(taskId, TaskState.CANCELLED, "Task cancellation requested");
                sendResult(task);
                updateExecutionState();
                log.info("Task {} cancelled while running (cooperative interrupt sent)", taskId);
                return true;
            }
        }
        return false;
    }

    private void rejectTask(WorkerTask task, String reason) {
        synchronized (task) {
            task.setState(TaskState.REJECTED);
            task.setErrorMessage(reason);
            task.setCompletedAt(Instant.now());
        }
        admissionService.release(task);
        log.warn("Task {} rejected: {}", task.getTaskId(), reason);
        sendStatus(task.getTaskId(), TaskState.REJECTED, reason);
        sendResult(task);
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WorkerTask task = taskQueue.take();
                
                synchronized (task) {
                    if (task.getState() == TaskState.CANCELLED) {
                        continue;
                    }
                }

                executeTask(task);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in queue processing loop", e);
            }
        }
    }

    private void executeTask(WorkerTask task) {
        synchronized (task) {
            if (task.getState() == TaskState.CANCELLED) {
                return;
            }
            task.setState(TaskState.RUNNING);
            task.setStartedAt(Instant.now());
        }
        updateExecutionState();
        sendStatus(task.getTaskId(), TaskState.RUNNING, "Task started");
        log.info("Task {} execution started", task.getTaskId());

        TaskHandler handler;
        try {
            handler = handlerRegistry.getHandler(task.getTaskType())
                    .orElseThrow(() -> new IllegalStateException("No handler found for " + task.getTaskType()));
        } catch (Exception e) {
            synchronized (task) {
                if (task.getState() != TaskState.CANCELLED) {
                    task.setState(TaskState.FAILED);
                    task.setErrorMessage(e.getMessage());
                    task.setCompletedAt(Instant.now());
                    admissionService.release(task);
                    sendStatus(task.getTaskId(), TaskState.FAILED, task.getErrorMessage());
                    sendResult(task);
                }
            }
            updateExecutionState();
            return;
        }

        Future<Object> future = taskExecutionPool.submit(() -> handler.execute(task));
        runningFutures.put(task.getTaskId(), future);

        try {
            Object result = future.get(task.getTimeoutSeconds(), TimeUnit.SECONDS);
            synchronized (task) {
                if (task.getState() != TaskState.CANCELLED) {
                    task.setResult(result);
                    task.setState(TaskState.COMPLETED);
                    task.setCompletedAt(Instant.now());
                    admissionService.release(task);
                    sendResult(task);
                    log.info("Task {} completed successfully", task.getTaskId());
                } else {
                    log.info("Task {} completed after being cancelled; result discarded", task.getTaskId());
                }
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            synchronized (task) {
                if (task.getState() != TaskState.CANCELLED) {
                    task.setState(TaskState.FAILED);
                    task.setErrorMessage("Task timed out after " + task.getTimeoutSeconds() + " seconds");
                    task.setCompletedAt(Instant.now());
                    admissionService.release(task);
                    sendStatus(task.getTaskId(), TaskState.FAILED, task.getErrorMessage());
                    sendResult(task);
                    log.warn("Task {} timed out after {} seconds", task.getTaskId(), task.getTimeoutSeconds());
                }
            }
        } catch (CancellationException e) {
            synchronized (task) {
                if (task.getState() != TaskState.CANCELLED) {
                    task.setState(TaskState.CANCELLED);
                    task.setErrorMessage("Task execution was cancelled");
                    task.setCompletedAt(Instant.now());
                    admissionService.release(task);
                    sendStatus(task.getTaskId(), TaskState.CANCELLED, task.getErrorMessage());
                    sendResult(task);
                }
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            synchronized (task) {
                if (task.getState() != TaskState.CANCELLED) {
                    if (cause instanceof InterruptedException) {
                        task.setState(TaskState.CANCELLED);
                        task.setErrorMessage("Task execution interrupted");
                    } else {
                        task.setState(TaskState.FAILED);
                        task.setErrorMessage(cause.getMessage() != null ? cause.getMessage() : "Unknown execution error");
                    }
                    task.setCompletedAt(Instant.now());
                    admissionService.release(task);
                    sendStatus(task.getTaskId(), task.getState(), task.getErrorMessage());
                    sendResult(task);
                    log.warn("Task {} failed during execution: {}", task.getTaskId(), task.getErrorMessage());
                }
            }
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            synchronized (task) {
                if (task.getState() != TaskState.CANCELLED) {
                    task.setState(TaskState.FAILED);
                    task.setErrorMessage("Task execution thread interrupted");
                    task.setCompletedAt(Instant.now());
                    admissionService.release(task);
                    sendStatus(task.getTaskId(), TaskState.FAILED, task.getErrorMessage());
                    sendResult(task);
                }
            }
        } finally {
            runningFutures.remove(task.getTaskId());
            updateExecutionState();
        }
    }

    private void updateExecutionState() {
        try {
            long runningCount = getActiveTasks().size();
            ExecutionState current = stateManager.getExecutionState();
            if (runningCount > 0) {
                if (current == ExecutionState.OFFLINE) {
                    stateManager.transitionExecution(ExecutionState.IDLE);
                }
                if (stateManager.getExecutionState() == ExecutionState.IDLE) {
                    stateManager.transitionExecution(ExecutionState.BUSY);
                }
            } else {
                if (current == ExecutionState.BUSY) {
                    stateManager.transitionExecution(ExecutionState.IDLE);
                }
            }
        } catch (Exception e) {
            log.debug("Execution state transition note: {}", e.getMessage());
        }
    }

    private void sendStatus(String taskId, TaskState state, String message) {
        TaskStatusMessage statusMsg = new TaskStatusMessage(taskId, state.name(), message);
        MessageEnvelope<TaskStatusMessage> env = MessageEnvelope.<TaskStatusMessage>builder()
                .type(MessageType.TASK_STATUS)
                .workerId(configStore != null ? configStore.getWorkerId() : "unknown")
                .timestamp(Instant.now())
                .payload(statusMsg)
                .build();
        connectionManager.sendMessage("/app/worker.task.status", env);
    }

    private void sendResult(WorkerTask task) {
        long duration = 0L;
        if (task.getStartedAt() != null && task.getCompletedAt() != null) {
            duration = Math.max(0L, Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis());
        } else if (task.getReceivedAt() != null && task.getCompletedAt() != null) {
            duration = Math.max(0L, Duration.between(task.getReceivedAt(), task.getCompletedAt()).toMillis());
        }

        TaskResultMessage resultMsg = TaskResultMessage.builder()
                .taskId(task.getTaskId())
                .status(task.getState().name())
                .result(task.getResult())
                .executionDurationMs(duration)
                .error(task.getErrorMessage())
                .jobId(task.getJobId())
                .partitionId(task.getPartitionId())
                .build();
        MessageEnvelope<TaskResultMessage> env = MessageEnvelope.<TaskResultMessage>builder()
                .type(MessageType.TASK_RESULT)
                .workerId(configStore != null ? configStore.getWorkerId() : "unknown")
                .timestamp(Instant.now())
                .payload(resultMsg)
                .build();
        connectionManager.sendMessage("/app/worker.task.result", env);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TaskService bounded pools...");
        dispatchPool.shutdownNow();
        taskExecutionPool.shutdownNow();
        try {
            if (!dispatchPool.awaitTermination(3, TimeUnit.SECONDS)) {
                log.warn("Dispatch pool did not terminate within timeout");
            }
            if (!taskExecutionPool.awaitTermination(3, TimeUnit.SECONDS)) {
                log.warn("Execution pool did not terminate within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
