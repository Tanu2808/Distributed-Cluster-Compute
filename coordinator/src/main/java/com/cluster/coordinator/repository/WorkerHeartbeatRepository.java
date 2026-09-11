package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.WorkerHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeat, Long> {
    void deleteByWorkerId(String workerId);
}
