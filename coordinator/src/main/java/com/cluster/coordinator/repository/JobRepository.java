package com.cluster.coordinator.repository;

import com.cluster.coordinator.model.Job;
import com.cluster.coordinator.model.JobState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByState(JobState state);
}
