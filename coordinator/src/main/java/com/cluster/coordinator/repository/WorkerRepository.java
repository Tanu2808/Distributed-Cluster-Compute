package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, String> {
    List<Worker> findByState(WorkerState state);
    List<Worker> findByLastHeartbeatBefore(LocalDateTime time);
}
