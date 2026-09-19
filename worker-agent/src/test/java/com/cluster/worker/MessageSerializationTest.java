package com.cluster.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cluster.shared.protocol.HeartbeatMessage;
import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.shared.protocol.MessageType;
import com.cluster.shared.protocol.TaskAssignmentMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MessageSerializationTest {

    @Autowired private ObjectMapper objectMapper;

    @Test
    public void testMessageEnvelopeInstantSerialization() throws Exception {
        MessageEnvelope<HeartbeatMessage> envelope =
                MessageEnvelope.<HeartbeatMessage>builder()
                        .type(MessageType.HEARTBEAT)
                        .workerId("worker-1")
                        .timestamp(Instant.parse("2026-09-18T10:00:00Z"))
                        .payload(new HeartbeatMessage("ONLINE", 2))
                        .build();

        String json = objectMapper.writeValueAsString(envelope);
        assertNotNull(json);

        MessageEnvelope<Map<String, Object>> deserialized =
                objectMapper.readValue(
                        json, new TypeReference<MessageEnvelope<Map<String, Object>>>() {});

        assertEquals(MessageType.HEARTBEAT, deserialized.getType());
        assertEquals("worker-1", deserialized.getWorkerId());
        assertEquals(Instant.parse("2026-09-18T10:00:00Z"), deserialized.getTimestamp());

        HeartbeatMessage payload =
                objectMapper.convertValue(deserialized.getPayload(), HeartbeatMessage.class);
        assertEquals("ONLINE", payload.getStatus());
        assertEquals(2, payload.getRunningTasks());
    }

    @Test
    public void testTaskAssignmentMessageRoundTrip() throws Exception {
        TaskAssignmentMessage assignMsg =
                TaskAssignmentMessage.builder()
                        .taskId("task-1")
                        .jobId("job-1")
                        .taskType("compute")
                        .input(Map.of("key", "value"))
                        .requiredCpuCores(2)
                        .requiredMemoryMb(1024)
                        .build();

        MessageEnvelope<TaskAssignmentMessage> envelope =
                MessageEnvelope.<TaskAssignmentMessage>builder()
                        .type(MessageType.TASK_ASSIGN)
                        .workerId("worker-1")
                        .timestamp(Instant.now())
                        .payload(assignMsg)
                        .build();

        String json = objectMapper.writeValueAsString(envelope);
        assertNotNull(json);

        MessageEnvelope<Map<String, Object>> deserialized =
                objectMapper.readValue(
                        json, new TypeReference<MessageEnvelope<Map<String, Object>>>() {});

        TaskAssignmentMessage payload =
                objectMapper.convertValue(deserialized.getPayload(), TaskAssignmentMessage.class);
        assertEquals("task-1", payload.getTaskId());
        assertEquals(2, payload.getRequiredCpuCores());
        assertEquals("value", payload.getInput().get("key"));
    }
}
