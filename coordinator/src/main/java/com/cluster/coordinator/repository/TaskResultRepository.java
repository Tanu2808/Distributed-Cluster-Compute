package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.TaskResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskResultRepository extends JpaRepository<TaskResult, String> {
    Optional<TaskResult> findByTaskId(String taskId);
}
