package com.cluster.shared.protocol;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnvelope<T> {
    private MessageType type;
    private String workerId;
    private Instant timestamp;
    private T payload;
}
