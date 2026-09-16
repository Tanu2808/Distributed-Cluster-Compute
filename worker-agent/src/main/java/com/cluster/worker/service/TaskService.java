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
    
    private final BlockingQueue<WorkerTask> taskQueue;
    private final ExecutorService executorService;

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

        int maxConcurrent = config.getExecution().getMaxConcurrentTasks();
        this.taskQueue = new LinkedBlockingQueue<>(config.getExecution().getQueueCapacity());
        
        // Custom thread pool that polls the queue
        this.executorService = Executors.newFixedThreadPool(maxConcurrent);
        
        // Start background dispatch threads
        for (int i = 0; i < maxConcurrent; i++) {
            this.executorService.submit(this::processQueue);
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
        return Optional.ofNullable(allTasks.get(taskId));
    }

    public void submitTask(TaskAssignmentMessage assignment) {
        log.info("Received TASK_ASSIGN for ID: {}", assignment.getTaskId());

        // Check duplicates
        if (allTasks.containsKey(assignment.getTaskId())) {
            log.warn("Duplicate task ID received: {}", assignment.getTaskId());
            sendStatus(assignment.getTaskId(), TaskState.REJECTED, "Duplicate task ID");
            return;
        }

        WorkerTask task = new WorkerTask();
        task.setTaskId(assignment.getTaskId());
        task.setTaskType(assignment.getTaskType());
        task.setInput(assignment.getInput());
        task.setRequiredCpuCores(assignment.getRequiredCpuCores());
        task.setRequiredMemoryMb(assignment.getRequiredMemoryMb());
        task.setTimeoutSeconds(assignment.getTimeoutSeconds() > 0 ? 
                               assignment.getTimeoutSeconds() : 
                               config.getExecution().getDefaultTimeoutSeconds());
        
        allTasks.put(task.getTaskId(), task);
        task.setState(TaskState.VALIDATING);

        // Validate handler exists
        Optional<TaskHandler> handlerOpt = handlerRegistry.getHandler(task.getTaskType());
        if (handlerOpt.isEmpty()) {
            rejectTask(task, "Unsupported task type: " + task.getTaskType());
            return;
        }

        // Admission control
        synchronized (this) {
            long allocatedMem = getActiveTasks().stream().mapToLong(WorkerTask::getRequiredMemoryMb).sum();
            int allocatedCpu = getActiveTasks().stream().mapToInt(WorkerTask::getRequiredCpuCores).sum();
            
            boolean admitted = admissionService.canAdmit(task, allocatedMem, allocatedCpu, 
                                                         metricsProvider.collectMetrics(), 
                                                         config.getExecution());
            
            if (!admitted) {
                rejectTask(task, "Insufficient resources");
                return;
            }

            if (taskQueue.remainingCapacity() == 0) {
                rejectTask(task, "Worker queue is full");
                return;
            }

            task.setState(TaskState.QUEUED);
            taskQueue.offer(task);
            sendStatus(task.getTaskId(), TaskState.QUEUED, "Task queued successfully");
        }
    }
    
    public boolean cancelTask(String taskId) {
        WorkerTask task = allTasks.get(taskId);
        if (task == null) return false;
        
        synchronized (task) {
            if (task.getState() == TaskState.QUEUED) {
                taskQueue.remove(task);
                task.setState(TaskState.CANCELLED);
                sendStatus(taskId, TaskState.CANCELLED, "Task cancelled while in queue");
                return true;
            } else if (task.getState() == TaskState.RUNNING) {
                // For Phase 7, we don't have safe thread interruption implemented for deterministic logic 
                // However, we can mark it cancelled so status is reflected.
                // Ideally we'd use Future.cancel(true) but we're polling in a loop, not mapping Futures 1:1 in a map currently.
                task.setState(TaskState.CANCELLED);
                sendStatus(taskId, TaskState.CANCELLED, "Task cancellation requested");
                updateExecutionState();
                return true;
            }
        }
        return false;
    }

    private void rejectTask(WorkerTask task, String reason) {
        task.setState(TaskState.REJECTED);
        task.setErrorMessage(reason);
        log.warn("Task {} rejected: {}", task.getTaskId(), reason);
        sendStatus(task.getTaskId(), TaskState.REJECTED, reason);
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WorkerTask task = taskQueue.take();
                
                // If it was cancelled while in queue, skip execution
                if (task.getState() == TaskState.CANCELLED) {
                    continue;
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
        task.setState(TaskState.RUNNING);
        task.setStartedAt(Instant.now());
        updateExecutionState();
        sendStatus(task.getTaskId(), TaskState.RUNNING, "Task started");

        TaskHandler handler = handlerRegistry.getHandler(task.getTaskType()).orElseThrow();

        // We run the handler in another executor just to enforce timeouts safely without interrupting our worker loop
        ExecutorService timeoutExecutor = Executors.newSingleThreadExecutor();
        Future<Object> future = timeoutExecutor.submit(() -> handler.execute(task));

        try {
            Object result = future.get(task.getTimeoutSeconds(), TimeUnit.SECONDS);
            task.setResult(result);
            task.setState(TaskState.COMPLETED);
            task.setCompletedAt(Instant.now());
            sendResult(task);
            
        } catch (TimeoutException e) {
            future.cancel(true);
            task.setState(TaskState.FAILED);
            task.setErrorMessage("Task timed out after " + task.getTimeoutSeconds() + " seconds");
            task.setCompletedAt(Instant.now());
            sendStatus(task.getTaskId(), TaskState.FAILED, task.getErrorMessage());
            sendResult(task);
            
        } catch (Exception e) {
            task.setState(TaskState.FAILED);
            task.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown execution error");
            task.setCompletedAt(Instant.now());
            sendStatus(task.getTaskId(), TaskState.FAILED, task.getErrorMessage());
            sendResult(task);
            
        } finally {
            timeoutExecutor.shutdownNow();
            updateExecutionState();
        }
    }

    private void updateExecutionState() {
        long runningCount = getActiveTasks().size();
        if (runningCount > 0) {
            stateManager.transitionExecution(ExecutionState.BUSY);
        } else {
            stateManager.transitionExecution(ExecutionState.IDLE);
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
        long duration = Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis();
        TaskResultMessage resultMsg = new TaskResultMessage(
                task.getTaskId(),
                task.getState().name(),
                task.getResult(),
                duration,
                task.getErrorMessage()
        );
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
        executorService.shutdownNow();
    }
}
