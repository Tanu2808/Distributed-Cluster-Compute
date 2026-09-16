package com.cluster.worker.registration;

import com.cluster.worker.persistence.WorkerConfigurationStore;
import org.springframework.stereotype.Component;

@Component
public class WorkerIdentityGenerator {

    private final WorkerConfigurationStore configStore;

    public WorkerIdentityGenerator(WorkerConfigurationStore configStore) {
        this.configStore = configStore;
    }

    public String getOrCreateWorkerId() {
        return configStore.getWorkerId();
    }
}
