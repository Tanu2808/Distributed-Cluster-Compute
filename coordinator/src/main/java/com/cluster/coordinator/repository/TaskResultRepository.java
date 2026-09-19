package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.TaskResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskResultRepository extends JpaRepository<TaskResult, String> {
    Optional<TaskResult> findByTaskId(String taskId);

    java.util.List<TaskResult> findByJobId(String jobId);
}
