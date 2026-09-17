package com.cluster.coordinator.service.cluster;

import com.cluster.coordinator.model.ClusterSettings;
import com.cluster.coordinator.repository.ClusterSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
public class ClusterEnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(ClusterEnrollmentService.class);
    private static final String JOIN_CODE_KEY = "CLUSTER_JOIN_CODE";
    private static final String ALPHANUMERICS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom secureRandom = new SecureRandom();

    private final ClusterSettingsRepository settingsRepository;

    @Value("${server.port:8080}")
    private String serverPort;

    public ClusterEnrollmentService(ClusterSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Gets the current join code. If one does not exist, it generates and persists a new one.
     */
    @Transactional
    public String getJoinCode() {
        return settingsRepository.findById(JOIN_CODE_KEY)
                .map(ClusterSettings::getValue)
                .orElseGet(() -> {
                    String newCode = generateNewCode();
                    settingsRepository.save(new ClusterSettings(JOIN_CODE_KEY, newCode));
                    log.info("No existing cluster join code found. Generated a new one.");
                    return newCode;
                });
    }

    /**
     * Generates a new join code and replaces the existing one.
     */
    @Transactional
    public String rotateJoinCode() {
        String newCode = generateNewCode();
        settingsRepository.save(new ClusterSettings(JOIN_CODE_KEY, newCode));
        log.info("Cluster join code rotated.");
        return newCode;
    }

    /**
     * Validates a provided join code and returns the cluster connection information.
     */
    @Transactional(readOnly = true)
    public EnrollmentResult enrollWorker(String providedCode) {
        if (providedCode == null || providedCode.isBlank()) {
            return new EnrollmentResult(false, null, null);
        }

        // Must happen in a transaction for findById
        String currentCode = settingsRepository.findById(JOIN_CODE_KEY)
                .map(ClusterSettings::getValue)
                .orElse(null);
                
        if (currentCode != null && currentCode.equals(providedCode.trim().toUpperCase())) {
            // Note: clusterId logic. If the coordinator has a cluster ID stored, use it, else default.
            String clusterId = settingsRepository.findById("CLUSTER_ID")
                    .map(ClusterSettings::getValue)
                    .orElse("local-cluster");
            
            // Construct a relative or default URL. The worker might already be calling us on a specific URL.
            // For MVP, if they successfully connect, they'll just keep using whatever base URL they connected with.
            // But we must return the connection info expected by ClusterSetupService.
            String coordinatorUrl = "http://localhost:" + serverPort;

            return new EnrollmentResult(true, clusterId, coordinatorUrl);
        }

        return new EnrollmentResult(false, null, null);
    }

    private String generateNewCode() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHANUMERICS.charAt(secureRandom.nextInt(ALPHANUMERICS.length())));
        }
        return formatCode(sb.toString());
    }

    private String formatCode(String raw) {
        return raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16);
    }

    public static class EnrollmentResult {
        private final boolean success;
        private final String clusterId;
        private final String coordinatorUrl;

        public EnrollmentResult(boolean success, String clusterId, String coordinatorUrl) {
            this.success = success;
            this.clusterId = clusterId;
            this.coordinatorUrl = coordinatorUrl;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getClusterId() {
            return clusterId;
        }

        public String getCoordinatorUrl() {
            return coordinatorUrl;
        }
    }
}
