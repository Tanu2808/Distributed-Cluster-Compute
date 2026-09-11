package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.WorkerResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerResourceRepository extends JpaRepository<WorkerResource, Long> {
    Optional<WorkerResource> findByWorkerId(String workerId);
    void deleteByWorkerId(String workerId);
}
