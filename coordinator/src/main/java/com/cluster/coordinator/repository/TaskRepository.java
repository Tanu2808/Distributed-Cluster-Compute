package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.Task;
import com.cluster.coordinator.model.TaskState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByJobId(String jobId);

    List<Task> findByState(TaskState state);

    Optional<Task> findByJobIdAndPartitionId(String jobId, Integer partitionId);
}
