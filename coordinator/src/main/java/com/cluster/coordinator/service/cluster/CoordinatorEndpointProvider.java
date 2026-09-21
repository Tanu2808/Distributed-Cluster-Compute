package com.cluster.coordinator.service.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

@Service
public class CoordinatorEndpointProvider implements ApplicationListener<WebServerInitializedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorEndpointProvider.class);

    @Value("${cluster.coordinator.advertised-host:}")
    private String configuredAdvertisedHost;

    private int actualPort = 0;
    private String detectedLanIp = null;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.actualPort = event.getWebServer().getPort();
        this.detectedLanIp = detectLanIp();

        log.info("=========================================================");
        log.info("Coordinator Endpoint Provider Initialized");
        log.info("Actual Port: {}", this.actualPort);
        log.info("Advertised Host: {}", getAdvertisedHost());
        log.info("HTTP Base URL: {}", getBaseUrl());
        log.info("WebSocket URL: {}", getWebSocketUrl());
        log.info("=========================================================");
    }

    public int getActualPort() {
        if (actualPort == 0) {
            log.warn("Actual port is requested but web server has not initialized yet. Returning 8080 as fallback.");
            return 8080;
        }
        return actualPort;
    }

    public String getAdvertisedHost() {
        if (StringUtils.hasText(configuredAdvertisedHost)) {
            return configuredAdvertisedHost;
        }
        if (detectedLanIp != null) {
            return detectedLanIp;
        }
        return "127.0.0.1";
    }

    public String getBaseUrl() {
        return "http://" + getAdvertisedHost() + ":" + getActualPort();
    }

    public String getWebSocketUrl() {
        return "ws://" + getAdvertisedHost() + ":" + getActualPort() + "/ws/coordinator";
    }

    private String detectLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // Skip loopback, inactive, or virtual interfaces (like docker0, veth) if possible
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }
                
                String name = iface.getName().toLowerCase();
                if (name.contains("docker") || name.contains("veth") || name.contains("vmnet") || name.contains("vboxnet") || name.contains("wsl")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // We prefer IPv4 for the demo
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String ip = addr.getHostAddress();
                        log.info("Detected suitable LAN IPv4 address: {} on interface: {}", ip, iface.getName());
                        return ip;
                    }
                }
            }
            
            // Fallback to local host if nothing else is found
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost instanceof Inet4Address && !localHost.isLoopbackAddress()) {
                return localHost.getHostAddress();
            }
            
        } catch (Exception e) {
            log.warn("Failed to detect LAN IP address", e);
        }
        log.warn("No suitable LAN IP found, falling back to 127.0.0.1");
        return "127.0.0.1";
    }
}
