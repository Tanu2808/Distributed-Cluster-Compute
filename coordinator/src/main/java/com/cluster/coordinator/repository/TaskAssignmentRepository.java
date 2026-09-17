package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.TaskAssignment;
import com.cluster.coordinator.model.AssignmentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, String> {
    List<TaskAssignment> findByWorkerId(String workerId);
    Optional<TaskAssignment> findByTaskId(String taskId);
    List<TaskAssignment> findByState(AssignmentState state);
    List<TaskAssignment> findByWorkerIdAndStateIn(String workerId, List<AssignmentState> states);
}
