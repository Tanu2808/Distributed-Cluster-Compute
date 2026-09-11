package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.ClusterEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClusterEventRepository extends JpaRepository<ClusterEvent, Long> {
    List<ClusterEvent> findTop100ByOrderByTimestampDesc();
}
