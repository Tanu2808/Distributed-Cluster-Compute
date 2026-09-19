package com.cluster.coordinator.controller;

import com.cluster.coordinator.dto.WorkerHeartbeatRequest;
import com.cluster.coordinator.dto.WorkerRegistrationRequest;
import com.cluster.coordinator.service.HeartbeatService;
import com.cluster.coordinator.service.WorkerService;
import com.cluster.shared.protocol.HeartbeatMessage;
import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.RegisterMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WorkerMessageController {

    private static final Logger log = LoggerFactory.getLogger(WorkerMessageController.class);

    private final WorkerService workerService;
    private final HeartbeatService heartbeatService;
    private final com.cluster.coordinator.service.TaskExecutionService taskExecutionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WorkerMessageController(
            WorkerService workerService,
            HeartbeatService heartbeatService,
            com.cluster.coordinator.service.TaskExecutionService taskExecutionService,
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        this.workerService = workerService;
        this.heartbeatService = heartbeatService;
        this.taskExecutionService = taskExecutionService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/worker.register")
    public void handleRegistration(@Payload MessageEnvelope<Map<String, Object>> envelope) {
        if (envelope.getType() != MessageType.REGISTER) {
            log.warn("Expected REGISTER message but got {}", envelope.getType());
            return;
        }

        try {
            RegisterMessage registerMessage =
                    objectMapper.convertValue(envelope.getPayload(), RegisterMessage.class);

            WorkerRegistrationRequest request = new WorkerRegistrationRequest();
            request.setId(envelope.getWorkerId());
            request.setName(registerMessage.getHostname());
            request.setHostname(registerMessage.getHostname());
            request.setOperatingSystem(registerMessage.getOsName());
            request.setAgentVersion("websocket-agent"); // Default for now
            request.setCpuCores(registerMessage.getCpuCores());
            request.setMemoryRamMb(registerMessage.getMemoryMb());

            workerService.registerWorker(request);

            log.info("Worker registered via WebSocket: {}", envelope.getWorkerId());

            // Send REGISTER_ACK
            MessageEnvelope<Void> ack =
                    MessageEnvelope.<Void>builder()
                            .type(MessageType.REGISTER_ACK)
                            .workerId(envelope.getWorkerId())
                            .timestamp(java.time.Instant.now())
                            .build();

            messagingTemplate.convertAndSend(
                    "/topic/worker." + envelope.getWorkerId() + ".control", ack);
        } catch (Exception e) {
            log.error("Failed to process registration for worker {}", envelope.getWorkerId(), e);
        }
    }

    @MessageMapping("/worker.heartbeat")
    public void handleHeartbeat(@Payload MessageEnvelope<Map<String, Object>> envelope) {
        if (envelope.getType() != MessageType.HEARTBEAT) {
            log.warn("Expected HEARTBEAT message but got {}", envelope.getType());
            return;
        }

        try {
            HeartbeatMessage heartbeatMessage =
                    objectMapper.convertValue(envelope.getPayload(), HeartbeatMessage.class);

            WorkerHeartbeatRequest request = new WorkerHeartbeatRequest();
            request.setCpuUsagePercent(0.0); // We will use ResourceUpdate for this later
            request.setMemoryUsagePercent(0.0);
            request.setActiveTasks(heartbeatMessage.getRunningTasks());

            heartbeatService.processHeartbeat(envelope.getWorkerId(), request);
        } catch (Exception e) {
            log.error("Failed to process heartbeat for worker {}", envelope.getWorkerId(), e);
        }
    }

    @MessageMapping("/worker.resource")
    public void handleResourceUpdate(@Payload MessageEnvelope<Map<String, Object>> envelope) {
        if (envelope.getType() != MessageType.RESOURCE_UPDATE) {
            log.warn("Expected RESOURCE_UPDATE message but got {}", envelope.getType());
            return;
        }

        try {
            com.cluster.shared.protocol.ResourceUpdateMessage resourceMessage =
                    objectMapper.convertValue(
                            envelope.getPayload(),
                            com.cluster.shared.protocol.ResourceUpdateMessage.class);

            // Update the resource capacity in WorkerResource
            workerService.updateWorkerResources(envelope.getWorkerId(), resourceMessage);

            // In a full implementation, we'd have a WorkerService method to update these dynamic
            // metrics.
            // For now, we update the heartbeat with cpu and memory.
            WorkerHeartbeatRequest request = new WorkerHeartbeatRequest();

            if (resourceMessage.getCpuUsagePercent() != null) {
                request.setCpuUsagePercent(resourceMessage.getCpuUsagePercent());
            } else {
                request.setCpuUsagePercent(0.0);
            }

            if (resourceMessage.getMemoryUsedBytes() != null
                    && resourceMessage.getMemoryTotalBytes() != null
                    && resourceMessage.getMemoryTotalBytes() > 0) {
                request.setMemoryUsagePercent(
                        (double) resourceMessage.getMemoryUsedBytes()
                                / resourceMessage.getMemoryTotalBytes()
                                * 100.0);
            } else {
                request.setMemoryUsagePercent(0.0);
            }

            request.setActiveTasks(0); // This should be tracked elsewhere

            heartbeatService.processHeartbeat(envelope.getWorkerId(), request);
        } catch (Exception e) {
            log.error("Failed to process resource update for worker {}", envelope.getWorkerId(), e);
        }
    }

    @MessageMapping("/worker.task.status")
    public void handleTaskStatus(@Payload MessageEnvelope<Map<String, Object>> envelope) {
        if (envelope.getType() != MessageType.TASK_STATUS) {
            log.warn("Expected TASK_STATUS message but got {}", envelope.getType());
            return;
        }

        try {
            com.cluster.shared.protocol.TaskStatusMessage statusMessage =
                    objectMapper.convertValue(
                            envelope.getPayload(),
                            com.cluster.shared.protocol.TaskStatusMessage.class);
            taskExecutionService.processTaskStatus(envelope.getWorkerId(), statusMessage);
        } catch (Exception e) {
            log.error("Failed to process task status from worker {}", envelope.getWorkerId(), e);
        }
    }

    @MessageMapping("/worker.task.result")
    public void handleTaskResult(@Payload MessageEnvelope<Map<String, Object>> envelope) {
        if (envelope.getType() != MessageType.TASK_RESULT) {
            log.warn("Expected TASK_RESULT message but got {}", envelope.getType());
            return;
        }

        try {
            com.cluster.shared.protocol.TaskResultMessage resultMessage =
                    objectMapper.convertValue(
                            envelope.getPayload(),
                            com.cluster.shared.protocol.TaskResultMessage.class);
            taskExecutionService.processTaskResult(envelope.getWorkerId(), resultMessage);
        } catch (Exception e) {
            log.error("Failed to process task result from worker {}", envelope.getWorkerId(), e);
        }
    }
}
