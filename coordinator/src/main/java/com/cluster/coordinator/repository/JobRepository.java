package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.JobState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByState(JobState state);
}
