package com.cluster.worker.api;

import com.cluster.worker.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveTasks() {
        return ResponseEntity.ok(Map.of("tasks", taskService.getActiveTasks()));
    }
}
