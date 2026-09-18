package com.cluster.worker.communication;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.worker.persistence.WorkerConfigurationStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Component
public class WebSocketConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionManager.class);

    private final WorkerConfigurationStore configStore;
    private final WebSocketStompClient stompClient;
    private final ThreadPoolTaskScheduler taskScheduler;
    private volatile StompSession stompSession;

    private final Object connectionLock = new Object();
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    
    // Diagnostics
    private volatile java.time.Instant connectedSince;
    private volatile java.time.Instant lastMessageTimestamp;
    private volatile String lastConnectionError;
    private volatile int reconnectCount = 0;

    private volatile Consumer<StompSession> onConnectCallback;
    private volatile Runnable onDisconnectCallback;

    public WebSocketConnectionManager(WorkerConfigurationStore configStore, @NonNull ObjectMapper objectMapper) {
        this.configStore = configStore;
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        this.stompClient.setMessageConverter(converter);
        
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setPoolSize(1);
        this.taskScheduler.setThreadNamePrefix("ws-stomp-");
        this.taskScheduler.initialize();
        this.stompClient.setTaskScheduler(taskScheduler);
        this.stompClient.setDefaultHeartbeat(new long[]{10000, 10000});
    }

    public void setCallbacks(Consumer<StompSession> onConnectCallback, Runnable onDisconnectCallback) {
        this.onConnectCallback = onConnectCallback;
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public boolean isConnected() {
        StompSession session = this.stompSession;
        return session != null && session.isConnected();
    }

    public java.time.Instant getConnectedSince() { return connectedSince; }
    public java.time.Instant getLastMessageTimestamp() { return lastMessageTimestamp; }
    public String getLastConnectionError() { return lastConnectionError; }
    public int getReconnectCount() { return reconnectCount; }
    public void resetReconnectCount() { reconnectCount = 0; }

    public void connect() {
        synchronized (connectionLock) {
            if (isConnected()) {
                log.debug("Already connected to coordinator STOMP endpoint");
                return;
            }

            if (!isConnecting.compareAndSet(false, true)) {
                log.debug("Connection attempt already in progress, skipping duplicate call");
                return;
            }
        }

        try {
            cleanupStaleSession();

            if (configStore == null || configStore.getConfig() == null || configStore.getConfig().getCoordinatorUrl() == null) {
                log.warn("Worker configuration or coordinator URL not available. Cannot connect.");
                this.lastConnectionError = "Missing coordinator configuration";
                handleDisconnect();
                return;
            }

            String coordinatorUrl = configStore.getConfig().getCoordinatorUrl();
            String wsUrl = coordinatorUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws/coordinator";
            
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            String workerId = configStore.getWorkerId();
            String credential = configStore.getConfig().getEnrollmentCredential();
            if (credential == null) {
                log.error("No enrollment credential available. Cannot connect.");
                this.lastConnectionError = "Missing enrollment credential";
                handleDisconnect();
                return;
            }

            String auth = workerId + ":" + credential;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.add("Authorization", authHeader);
            
            StompHeaders stompHeaders = new StompHeaders();

            log.info("Connecting to WebSocket at {}", wsUrl);
            reconnectCount++;

            StompSession newSession = stompClient.connectAsync(wsUrl, headers, stompHeaders, new StompSessionHandler())
                    .get(5, TimeUnit.SECONDS);

            synchronized (connectionLock) {
                this.stompSession = newSession;
                this.connectedSince = java.time.Instant.now();
                this.lastConnectionError = null;
            }
            
            log.info("Connected successfully to WebSocket coordinator");
            Consumer<StompSession> callback = this.onConnectCallback;
            if (callback != null) {
                callback.accept(newSession);
            }
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket: {}", e.getMessage());
            this.lastConnectionError = e.getMessage();
            cleanupStaleSession();
            handleDisconnect();
        } finally {
            isConnecting.set(false);
        }
    }

    public void sendMessage(@NonNull String destination, @NonNull MessageEnvelope<?> envelope) {
        StompSession session;
        synchronized (connectionLock) {
            session = this.stompSession;
        }
        if (session != null && session.isConnected()) {
            try {
                synchronized (session) {
                    session.send(destination, envelope);
                }
                this.lastMessageTimestamp = java.time.Instant.now();
            } catch (Exception e) {
                log.error("Error sending message to {}: ", destination, e);
                this.lastConnectionError = "Send error: " + e.getMessage();
                handleDisconnect();
            }
        } else {
            log.warn("Cannot send message, WebSocket is not connected (destination: {})", destination);
            this.lastConnectionError = "WebSocket not connected when sending message";
        }
    }

    public void disconnect() {
        cleanupStaleSession();
        handleDisconnect();
    }

    private void cleanupStaleSession() {
        synchronized (connectionLock) {
            if (stompSession != null) {
                try {
                    if (stompSession.isConnected()) {
                        stompSession.disconnect();
                    }
                } catch (Exception e) {
                    log.debug("Error disconnecting stale STOMP session: {}", e.getMessage());
                }
                stompSession = null;
            }
            connectedSince = null;
        }
    }

    private void handleDisconnect() {
        Runnable callback = this.onDisconnectCallback;
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                log.error("Error in onDisconnect callback: {}", e.getMessage(), e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocketConnectionManager...");
        cleanupStaleSession();
        try {
            stompClient.stop();
        } catch (Exception e) {
            log.debug("Error stopping stomp client: {}", e.getMessage());
        }
        try {
            taskScheduler.shutdown();
        } catch (Exception e) {
            log.debug("Error shutting down STOMP task scheduler: {}", e.getMessage());
        }
    }

    private class StompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void handleTransportError(@NonNull StompSession session, @NonNull Throwable exception) {
            synchronized (connectionLock) {
                if (session != stompSession && stompSession != null) {
                    log.debug("Ignoring transport error from obsolete session");
                    return;
                }
            }
            log.error("Transport error: {}", exception.getMessage());
            lastConnectionError = "Transport error: " + exception.getMessage();
            cleanupStaleSession();
            handleDisconnect();
        }

        @Override
        public void handleException(@NonNull StompSession session, StompCommand command, @NonNull StompHeaders headers, @NonNull byte[] payload, @NonNull Throwable exception) {
            synchronized (connectionLock) {
                if (session != stompSession && stompSession != null) {
                    log.debug("Ignoring exception from obsolete session");
                    return;
                }
            }
            log.error("STOMP protocol exception: {}", exception.getMessage());
            lastConnectionError = "STOMP error: " + exception.getMessage();
            cleanupStaleSession();
            handleDisconnect();
        }
        
        @Override
        public void afterConnected(@NonNull StompSession session, @NonNull StompHeaders connectedHeaders) {
            log.info("STOMP session handshake complete");
        }
    }
}
