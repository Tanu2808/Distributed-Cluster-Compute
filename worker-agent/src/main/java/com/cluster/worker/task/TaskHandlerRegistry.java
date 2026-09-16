package com.cluster.worker.task;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskHandlerRegistry {

    private final List<TaskHandler> handlers;

    public TaskHandlerRegistry(List<TaskHandler> handlers) {
        this.handlers = handlers;
    }

    public Optional<TaskHandler> getHandler(String taskType) {
        return handlers.stream()
                .filter(handler -> handler.supports(taskType))
                .findFirst();
    }
}
