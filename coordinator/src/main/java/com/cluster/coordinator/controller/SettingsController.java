package com.cluster.coordinator.controller;

import com.cluster.coordinator.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getSettings() {
        return ResponseEntity.ok(settingsService.getAllSettings());
    }

    @PutMapping
    public ResponseEntity<Void> updateSettings(@RequestBody Map<String, String> settings) {
        settingsService.updateSettings(settings);
        return ResponseEntity.ok().build();
    }
}
