package com.cluster.coordinator.controller;

import com.cluster.coordinator.dto.WorkerHeartbeatRequest;
import com.cluster.coordinator.service.HeartbeatService;
import com.cluster.coordinator.service.WorkerService;
import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.ResourceUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class WorkerMessageControllerTest {

    @Mock
    private WorkerService workerService;

    @Mock
    private HeartbeatService heartbeatService;

    @Mock
    private com.cluster.coordinator.service.TaskExecutionService taskExecutionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;

    private WorkerMessageController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        controller = new WorkerMessageController(workerService, heartbeatService, taskExecutionService, messagingTemplate, objectMapper);
    }

    @Test
    void handleResourceUpdate_WithValidValues_UpdatesResourcesAndHeartbeat() {
        ResourceUpdateMessage msg = new ResourceUpdateMessage();
        msg.setCpuUsagePercent(45.5);
        msg.setMemoryUsedBytes(2048L * 1024 * 1024);
        msg.setMemoryTotalBytes(8192L * 1024 * 1024);
        msg.setDiskTotalBytes(100000L * 1024 * 1024);

        Map<String, Object> payload = objectMapper.convertValue(msg, Map.class);
        MessageEnvelope<Map<String, Object>> envelope = MessageEnvelope.<Map<String, Object>>builder()
                .type(MessageType.RESOURCE_UPDATE)
                .workerId("worker-1")
                .payload(payload)
                .build();

        controller.handleResourceUpdate(envelope);

        ArgumentCaptor<ResourceUpdateMessage> resourceCaptor = ArgumentCaptor.forClass(ResourceUpdateMessage.class);
        verify(workerService).updateWorkerResources(eq("worker-1"), resourceCaptor.capture());
        
        ResourceUpdateMessage capturedMsg = resourceCaptor.getValue();
        assertEquals(45.5, capturedMsg.getCpuUsagePercent());
        assertEquals(msg.getMemoryTotalBytes(), capturedMsg.getMemoryTotalBytes());
        assertEquals(msg.getDiskTotalBytes(), capturedMsg.getDiskTotalBytes());

        ArgumentCaptor<WorkerHeartbeatRequest> heartbeatCaptor = ArgumentCaptor.forClass(WorkerHeartbeatRequest.class);
        verify(heartbeatService).processHeartbeat(eq("worker-1"), heartbeatCaptor.capture());
        
        WorkerHeartbeatRequest hb = heartbeatCaptor.getValue();
        assertEquals(45.5, hb.getCpuUsagePercent());
        assertEquals(25.0, hb.getMemoryUsagePercent());
    }

    @Test
    void handleResourceUpdate_WithNullValues_DoesNotThrowNPE() {
        ResourceUpdateMessage msg = new ResourceUpdateMessage();
        // Null CPU, missing memory
        
        Map<String, Object> payload = objectMapper.convertValue(msg, Map.class);
        MessageEnvelope<Map<String, Object>> envelope = MessageEnvelope.<Map<String, Object>>builder()
                .type(MessageType.RESOURCE_UPDATE)
                .workerId("worker-1")
                .payload(payload)
                .build();

        controller.handleResourceUpdate(envelope);

        ArgumentCaptor<WorkerHeartbeatRequest> heartbeatCaptor = ArgumentCaptor.forClass(WorkerHeartbeatRequest.class);
        verify(heartbeatService).processHeartbeat(eq("worker-1"), heartbeatCaptor.capture());
        
        WorkerHeartbeatRequest hb = heartbeatCaptor.getValue();
        assertEquals(0.0, hb.getCpuUsagePercent());
        assertEquals(0.0, hb.getMemoryUsagePercent());
    }
}
