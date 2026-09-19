package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.Worker;
import com.cluster.coordinator.model.WorkerState;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, String> {
    List<Worker> findByState(WorkerState state);

    List<Worker> findByLastHeartbeatBefore(LocalDateTime time);
}
