package com.cluster.coordinator.service;

import com.cluster.coordinator.model.ClusterSettings;
import com.cluster.coordinator.repository.ClusterSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private final ClusterSettingsRepository settingsRepository;
    private final EventService eventService;

    public SettingsService(ClusterSettingsRepository settingsRepository, EventService eventService) {
        this.settingsRepository = settingsRepository;
        this.eventService = eventService;
    }

    public Map<String, String> getAllSettings() {
        return settingsRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getKey(), s -> s.getValue()));
    }

    @Transactional
    public void updateSettings(Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            ClusterSettings setting = settingsRepository.findById(entry.getKey())
                    .orElse(new ClusterSettings(entry.getKey(), ""));
            setting.setValue(entry.getValue());
            settingsRepository.save(setting);
        }
        eventService.recordEvent("SETTINGS_CHANGED", "Cluster settings updated", null);
    }
}
