package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.ClusterSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClusterSettingsRepository extends JpaRepository<ClusterSettings, String> {}
