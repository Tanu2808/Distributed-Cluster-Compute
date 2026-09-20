package com.cluster.coordinator.config;

import java.awt.Desktop;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class BrowserLauncher implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    @Value("${coordinator.ui.auto-open:false}")
    private boolean autoOpen;

    @Value("${server.port:8080}")
    private String port;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (autoOpen) {
            String url = "http://localhost:" + port;
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                    log.info("Auto-opened Coordinator UI at {}", url);
                } else {
                    log.info("Desktop browsing is not supported. Coordinator UI is available at {}", url);
                }
            } catch (Exception e) {
                log.warn("Failed to auto-open Coordinator UI: {}. It is available at {}", e.getMessage(), url);
            }
        }
    }
}
