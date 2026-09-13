package com.cluster.worker.communication;

import com.cluster.shared.protocol.MessageEnvelope;
import com.cluster.worker.config.WorkerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class WebSocketConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConnectionManager.class);

    private final WorkerConfig config;
    private final WebSocketStompClient stompClient;
    private StompSession stompSession;
    

    private Consumer<StompSession> onConnectCallback;
    private Runnable onDisconnectCallback;

    public WebSocketConnectionManager(WorkerConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        this.stompClient.setMessageConverter(converter);
        
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler taskScheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        taskScheduler.initialize();
        this.stompClient.setTaskScheduler(taskScheduler);
        this.stompClient.setDefaultHeartbeat(new long[]{10000, 10000});
    }

    public void setCallbacks(Consumer<StompSession> onConnectCallback, Runnable onDisconnectCallback) {
        this.onConnectCallback = onConnectCallback;
        this.onDisconnectCallback = onDisconnectCallback;
    }

    public void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }

        String wsUrl = config.getCoordinator().getUrl().replace("http://", "ws://").replace("https://", "wss://") + "/ws/coordinator";
        
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        // The api-key will be used as basic auth for STOMP or WS handshake
        String auth = "admin:" + config.getCoordinator().getApiKey();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.add("Authorization", authHeader);
        
        StompHeaders stompHeaders = new StompHeaders();

        try {
            log.info("Connecting to WebSocket: {}", wsUrl);
            this.stompSession = stompClient.connectAsync(wsUrl, headers, stompHeaders, new StompSessionHandler()).get(5, TimeUnit.SECONDS);
            log.info("Connected successfully to WebSocket");
            if (onConnectCallback != null) {
                onConnectCallback.accept(this.stompSession);
            }
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket: {}", e.getMessage(), e);
            handleDisconnect();
        }
    }

    public void sendMessage(String destination, MessageEnvelope<?> envelope) {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.send(destination, envelope);
        } else {
            log.warn("Cannot send message, WebSocket is not connected");
            handleDisconnect();
        }
    }

    private void handleDisconnect() {
        if (onDisconnectCallback != null) {
            onDisconnectCallback.run();
        }
    }

    private class StompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void handleTransportError(@org.springframework.lang.NonNull StompSession session, @org.springframework.lang.NonNull Throwable exception) {
            log.error("Transport error: {}", exception.getMessage());
            handleDisconnect();
        }

        @Override
        public void handleException(@org.springframework.lang.NonNull StompSession session, @org.springframework.lang.Nullable StompCommand command, @org.springframework.lang.NonNull StompHeaders headers, @org.springframework.lang.NonNull byte[] payload, @org.springframework.lang.NonNull Throwable exception) {
            log.error("Stomp exception: {}", exception.getMessage());
            handleDisconnect();
        }
        
        @Override
        public void afterConnected(@org.springframework.lang.NonNull StompSession session, @org.springframework.lang.NonNull StompHeaders connectedHeaders) {
            log.info("STOMP session connected");
        }
    }
}
