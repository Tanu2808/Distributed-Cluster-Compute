package com.cluster.coordinator.config;

import com.cluster.coordinator.websocket.BasicAuthHandshakeInterceptor;
import com.cluster.coordinator.websocket.ClusterWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ClusterWebSocketConfig implements WebSocketConfigurer {

    private final ClusterWebSocketHandler clusterWebSocketHandler;

    @Value("${cluster.security.api-username:admin}")
    private String username;

    @Value("${cluster.security.api-password:admin_secret}")
    private String password;

    public ClusterWebSocketConfig(ClusterWebSocketHandler clusterWebSocketHandler) {
        this.clusterWebSocketHandler = clusterWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clusterWebSocketHandler, "/ws/cluster")
                .addInterceptors(new BasicAuthHandshakeInterceptor(username, password))
                .setAllowedOrigins("*");
    }
}
