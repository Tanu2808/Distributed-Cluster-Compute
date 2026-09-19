package com.cluster.worker.api;

import com.cluster.worker.api.dto.TaskSummaryResponse;
import com.cluster.worker.service.TaskService;
import java.util.List;
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

    @GetMapping
    public ResponseEntity<List<com.cluster.worker.task.WorkerTask>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/active")
    public ResponseEntity<TaskSummaryResponse> getActiveTasks() {
        return ResponseEntity.ok(new TaskSummaryResponse(taskService.getActiveTasks()));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<com.cluster.worker.task.WorkerTask> getTask(
            @org.springframework.web.bind.annotation.PathVariable String taskId) {
        return taskService
                .getTask(taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @org.springframework.web.bind.annotation.PostMapping("/{taskId}/cancel")
    public ResponseEntity<Void> cancelTask(
            @org.springframework.web.bind.annotation.PathVariable String taskId) {
        boolean cancelled = taskService.cancelTask(taskId);
        if (cancelled) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}
