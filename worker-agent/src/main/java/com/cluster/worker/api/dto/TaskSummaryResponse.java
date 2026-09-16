package com.cluster.worker.api.dto;
import java.util.List;
import com.cluster.worker.task.WorkerTask;
public record TaskSummaryResponse(List<WorkerTask> tasks) {}
