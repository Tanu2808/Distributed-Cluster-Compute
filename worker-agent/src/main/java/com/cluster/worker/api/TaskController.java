package com.cluster.worker.api;

import com.cluster.worker.service.TaskService;
import com.cluster.worker.api.dto.TaskSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/active")
    public ResponseEntity<TaskSummaryResponse> getActiveTasks() {
        return ResponseEntity.ok(new TaskSummaryResponse(taskService.getActiveTasks()));
    }
}
