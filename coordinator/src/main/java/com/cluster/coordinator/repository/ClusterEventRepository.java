package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.ClusterEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterEventRepository extends JpaRepository<ClusterEvent, Long> {
    List<ClusterEvent> findTop100ByOrderByTimestampDesc();
}
