package com.cluster.worker.monitoring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import oshi.SystemInfo;

@Configuration
public class SystemInfoConfig {

    @Bean
    public SystemInfo systemInfo() {
        return new SystemInfo();
    }
}
