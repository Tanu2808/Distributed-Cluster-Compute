package com.cluster.worker.api.dto;

import com.cluster.worker.task.WorkerTask;
import java.util.List;

public record TaskSummaryResponse(List<WorkerTask> tasks) {}
