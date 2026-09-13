package com.cluster.coordinator.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class BasicAuthHandshakeInterceptor implements HandshakeInterceptor {

    private final String expectedUsername;
    private final String expectedPassword;

    public BasicAuthHandshakeInterceptor(String expectedUsername, String expectedPassword) {
        this.expectedUsername = expectedUsername;
        this.expectedPassword = expectedPassword;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null && !protocols.isEmpty()) {
            String combined = String.join(",", protocols);
            String[] parts = combined.split(",");
            if (parts.length >= 2 && "basic".equalsIgnoreCase(parts[0].trim())) {
                String base64Credentials = parts[1].trim();
                try {
                    String credentials = new String(Base64.getDecoder().decode(base64Credentials));
                    String[] credParts = credentials.split(":", 2);
                    if (credParts.length == 2) {
                        if (expectedUsername.equals(credParts[0]) && expectedPassword.equals(credParts[1])) {
                            response.getHeaders().set("Sec-WebSocket-Protocol", "basic"); // Must echo accepted protocol
                            return true;
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid base64
                }
            }
        }
        
        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
