package com.cluster.shared.protocol;

public enum MessageType {
    REGISTER,
    REGISTER_ACK,
    HEARTBEAT,
    RESOURCE_UPDATE,
    TASK_ASSIGN,
    TASK_CANCEL,
    TASK_STATUS,
    TASK_RESULT,
    ERROR,
    PING,
    PONG
}
