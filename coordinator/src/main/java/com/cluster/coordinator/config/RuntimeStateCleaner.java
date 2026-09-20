package com.cluster.coordinator.config;

import com.cluster.coordinator.repository.ClusterEventRepository;
import com.cluster.coordinator.repository.JobRepository;
import com.cluster.coordinator.repository.TaskAssignmentRepository;
import com.cluster.coordinator.repository.TaskRepository;
import com.cluster.coordinator.repository.TaskResultRepository;
import com.cluster.coordinator.repository.WorkerHeartbeatRepository;
import com.cluster.coordinator.repository.WorkerRepository;
import com.cluster.coordinator.repository.WorkerResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RuntimeStateCleaner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeStateCleaner.class);

    private final TaskResultRepository taskResultRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskRepository taskRepository;
    private final JobRepository jobRepository;
    private final WorkerHeartbeatRepository workerHeartbeatRepository;
    private final WorkerResourceRepository workerResourceRepository;
    private final ClusterEventRepository clusterEventRepository;
    private final WorkerRepository workerRepository;

    public RuntimeStateCleaner(
            TaskResultRepository taskResultRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            TaskRepository taskRepository,
            JobRepository jobRepository,
            WorkerHeartbeatRepository workerHeartbeatRepository,
            WorkerResourceRepository workerResourceRepository,
            ClusterEventRepository clusterEventRepository,
            WorkerRepository workerRepository) {
        this.taskResultRepository = taskResultRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.taskRepository = taskRepository;
        this.jobRepository = jobRepository;
        this.workerHeartbeatRepository = workerHeartbeatRepository;
        this.workerResourceRepository = workerResourceRepository;
        this.clusterEventRepository = clusterEventRepository;
        this.workerRepository = workerRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logger.info("Initializing Coordinator: Wiping previous runtime state...");

        // Order is important due to foreign key constraints
        taskResultRepository.deleteAllInBatch();
        taskAssignmentRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
        jobRepository.deleteAllInBatch();
        
        workerHeartbeatRepository.deleteAllInBatch();
        workerResourceRepository.deleteAllInBatch();
        workerRepository.deleteAllInBatch();
        
        clusterEventRepository.deleteAllInBatch();

        logger.info("Runtime state successfully cleared. Coordinator is starting fresh.");
    }
}
