package com.cluster.worker.monitoring;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Primary
@Component
public class NvidiaGpuMetricsProvider implements GpuMetricsProvider {

    @Override
    public int getGpuCount() {
        try {
            Process process = new ProcessBuilder("nvidia-smi", "--query-gpu=count", "--format=csv,noheader")
                    .start();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return Integer.parseInt(line.trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // nvidia-smi not available or failed, fallback to 0
        }
        return 0;
    }

    @Override
    public String getGpuInfo() {
        int count = getGpuCount();
        if (count <= 0) {
            return "Unavailable";
        }
        try {
            Process process = new ProcessBuilder("nvidia-smi", "--query-gpu=name", "--format=csv,noheader")
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return line.trim();
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // nvidia-smi failed or not installed
        }
        return count + " NVIDIA GPU(s)";
    }
}
