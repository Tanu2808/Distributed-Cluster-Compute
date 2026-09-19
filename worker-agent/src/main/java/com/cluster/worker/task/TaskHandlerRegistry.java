package com.cluster.worker.task;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Service registry that maps incoming task types to their corresponding {@link TaskHandler} implementations.
 * Enables dynamic dispatch of distributed tasks to the appropriate physical execution logic.
 */
@Service
public class TaskHandlerRegistry {

    private final List<TaskHandler> handlers;

    public TaskHandlerRegistry(List<TaskHandler> handlers) {
        this.handlers = handlers;
    }

    public Optional<TaskHandler> getHandler(String taskType) {
        return handlers.stream().filter(handler -> handler.supports(taskType)).findFirst();
    }
}
