package com.cluster.worker.communication;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.TaskAssignmentMessage;
import com.cluster.worker.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;

@Component
public class TaskMessageHandler implements StompFrameHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskMessageHandler.class);
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public TaskMessageHandler(TaskService taskService, ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    @Override
    @org.springframework.lang.NonNull
    public Type getPayloadType(@org.springframework.lang.NonNull StompHeaders headers) {
        return MessageEnvelope.class;
    }

    @Override
    public void handleFrame(@org.springframework.lang.NonNull StompHeaders headers, @org.springframework.lang.Nullable Object payload) {
        if (payload instanceof MessageEnvelope) {
            MessageEnvelope<?> envelope = (MessageEnvelope<?>) payload;
            
            if (envelope.getType() == MessageType.TASK_ASSIGN) {
                try {
                    // Convert payload to TaskAssignmentMessage since Jackson might parse it as a LinkedHashMap in a generic Envelope
                    TaskAssignmentMessage assignMsg = objectMapper.convertValue(envelope.getPayload(), TaskAssignmentMessage.class);
                    taskService.submitTask(assignMsg);
                } catch (Exception e) {
                    log.error("Failed to parse TASK_ASSIGN message", e);
                }
            } else if (envelope.getType() == MessageType.TASK_CANCEL) {
                try {
                    // Expecting taskId in payload or simple Map
                    String taskId = null;
                    if (envelope.getPayload() instanceof Map) {
                        taskId = (String) ((Map<?, ?>) envelope.getPayload()).get("taskId");
                    } else if (envelope.getPayload() instanceof String) {
                        taskId = (String) envelope.getPayload();
                    }
                    if (taskId != null) {
                        taskService.cancelTask(taskId);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse TASK_CANCEL message", e);
                }
            }
        }
    }
}
