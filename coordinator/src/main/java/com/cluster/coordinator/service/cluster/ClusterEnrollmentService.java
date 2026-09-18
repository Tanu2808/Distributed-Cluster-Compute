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
    private final com.cluster.coordinator.repository.WorkerRepository workerRepository;

    @Value("${server.port:8080}")
    private String serverPort;

    public ClusterEnrollmentService(ClusterSettingsRepository settingsRepository, com.cluster.coordinator.repository.WorkerRepository workerRepository) {
        this.settingsRepository = settingsRepository;
        this.workerRepository = workerRepository;
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
     * <p>
     * Comparison is case-insensitive and formatting-independent: hyphens and whitespace
     * are stripped from both the stored value and the supplied value before comparing.
     * This means BTMV-WU8W-Y0HW-ZEXU, BTMVWU8WY0HWZEXU, and btmv wu8w y0hw zexu
     * all represent the same logical join code.
     */
    @Transactional
    public EnrollmentResult enrollWorker(String providedCode, String workerId) {
        if (providedCode == null || providedCode.isBlank() || workerId == null || workerId.isBlank()) {
            return new EnrollmentResult(false, null, null, null);
        }

        // Must happen in a transaction for findById
        String currentCode = settingsRepository.findById(JOIN_CODE_KEY)
                .map(ClusterSettings::getValue)
                .orElse(null);

        // Normalize both sides so that XXXX-XXXX-XXXX-XXXX and XXXXXXXXXXXXXXXX
        // are treated as identical logical values.
        if (currentCode != null && normalizeCode(currentCode).equals(normalizeCode(providedCode))) {
            // Note: clusterId logic. If the coordinator has a cluster ID stored, use it, else default.
            String clusterId = settingsRepository.findById("CLUSTER_ID")
                    .map(ClusterSettings::getValue)
                    .orElse("local-cluster");

            // Construct a relative or default URL. The worker might already be calling us on a specific URL.
            // For MVP, if they successfully connect, they'll just keep using whatever base URL they connected with.
            // But we must return the connection info expected by ClusterSetupService.
            String coordinatorUrl = "http://localhost:" + serverPort;

            // Generate runtime credential
            String rawCredential = generateRandomCredential();
            String hash = hashCredential(rawCredential);

            // Fetch or create worker
            com.cluster.coordinator.model.Worker worker = workerRepository.findById(workerId)
                    .orElseGet(() -> new com.cluster.coordinator.model.Worker(workerId, "unknown", com.cluster.coordinator.model.WorkerState.REGISTERING));
            
            worker.setRuntimeCredentialHash(hash);
            workerRepository.save(worker);

            return new EnrollmentResult(true, clusterId, coordinatorUrl, rawCredential);
        }

        return new EnrollmentResult(false, null, null, null);
    }

    /**
     * Normalizes a join code for comparison purposes by removing hyphens and whitespace
     * and converting to uppercase. Does not modify how codes are stored or displayed.
     */
    private String normalizeCode(String code) {
        return code.replaceAll("[\\s\\-]", "").toUpperCase();
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

    private String generateRandomCredential() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashCredential(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encodedhash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not found", e);
        }
    }

    public static class EnrollmentResult {
        private final boolean success;
        private final String clusterId;
        private final String coordinatorUrl;
        private final String runtimeCredential;

        public EnrollmentResult(boolean success, String clusterId, String coordinatorUrl, String runtimeCredential) {
            this.success = success;
            this.clusterId = clusterId;
            this.coordinatorUrl = coordinatorUrl;
            this.runtimeCredential = runtimeCredential;
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
        
        public String getRuntimeCredential() {
            return runtimeCredential;
        }
    }
}
