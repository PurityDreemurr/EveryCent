package com.everycent.qqbot;

import com.everycent.config.QqBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QqOfficialGatewayClient implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(QqOfficialGatewayClient.class);
    private static final int OP_DISPATCH = 0;
    private static final int OP_HEARTBEAT = 1;
    private static final int OP_IDENTIFY = 2;
    private static final int OP_RECONNECT = 7;
    private static final int OP_INVALID_SESSION = 9;
    private static final int OP_HELLO = 10;
    private static final long RECONNECT_DELAY_SECONDS = 5;

    private final QqBotProperties properties;
    private final QqOfficialBotClient botClient;
    private final QqBotAssistantBridgeService bridgeService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService messageExecutor = Executors.newFixedThreadPool(2);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile WebSocket webSocket;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile Long lastSequence;

    public QqOfficialGatewayClient(
        QqBotProperties properties,
        QqOfficialBotClient botClient,
        QqBotAssistantBridgeService bridgeService,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.botClient = botClient;
        this.bridgeService = bridgeService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void start() {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getAppId()) || !StringUtils.hasText(properties.getAppSecret())) {
            return;
        }
        if (running.compareAndSet(false, true)) {
            connectLater(0);
        }
    }

    @Override
    public void stop() {
        running.set(false);
        stopHeartbeat();
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "EveryCent shutdown");
        }
        scheduler.shutdownNow();
        messageExecutor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    private void connectLater(long delaySeconds) {
        if (!running.get()) {
            return;
        }
        scheduler.schedule(this::connect, delaySeconds, TimeUnit.SECONDS);
    }

    private void connect() {
        if (!running.get()) {
            return;
        }
        try {
            String gatewayUrl = botClient.gatewayUrl();
            LOG.info("Connecting QQ official bot gateway.");
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build()
                .newWebSocketBuilder()
                .header("Authorization", "QQBot " + botClient.accessToken())
                .buildAsync(URI.create(gatewayUrl), new GatewayListener())
                .thenAccept(socket -> this.webSocket = socket)
                .exceptionally(error -> {
                    LOG.warn("QQ official bot gateway connect failed: {}", error.getMessage());
                    connectLater(RECONNECT_DELAY_SECONDS);
                    return null;
                });
        } catch (RuntimeException e) {
            LOG.warn("QQ official bot gateway connect failed: {}", e.getMessage());
            connectLater(RECONNECT_DELAY_SECONDS);
        }
    }

    private void handlePayload(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            int op = root.path("op").asInt(-1);
            if (root.path("s").canConvertToLong()) {
                lastSequence = root.path("s").asLong();
            }
            if (op == OP_HELLO) {
                handleHello(root.path("d"));
            } else if (op == OP_DISPATCH) {
                handleDispatch(root);
            } else if (op == OP_RECONNECT || op == OP_INVALID_SESSION) {
                reconnect();
            }
        } catch (Exception e) {
            LOG.warn("QQ official bot gateway payload handling failed: {}", e.getMessage());
        }
    }

    private void handleHello(JsonNode data) {
        long interval = data.path("heartbeat_interval").asLong(45000L);
        sendIdentify();
        startHeartbeat(interval);
    }

    private void handleDispatch(JsonNode root) throws Exception {
        QqOfficialEvent event = new QqOfficialEvent();
        event.setOp(root.path("op").asInt());
        event.setId(root.path("id").asText(null));
        event.setT(root.path("t").asText(null));
        event.setD(root.path("d"));
        messageExecutor.execute(() -> bridgeService.handleEvent(event));
    }

    private void sendIdentify() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", "QQBot " + botClient.accessToken());
        data.put("intents", properties.getGatewayIntents() == null ? 0 : properties.getGatewayIntents());
        data.put("shard", java.util.List.of(0, 1));
        data.put("properties", Map.of("os", "linux", "browser", "everycent", "device", "everycent"));
        sendJson(OP_IDENTIFY, data);
    }

    private void startHeartbeat(long intervalMillis) {
        stopHeartbeat();
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> sendJson(OP_HEARTBEAT, lastSequence), intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeat() {
        ScheduledFuture<?> task = heartbeatTask;
        if (task != null) {
            task.cancel(true);
            heartbeatTask = null;
        }
    }

    private void reconnect() {
        stopHeartbeat();
        WebSocket socket = webSocket;
        if (socket != null) {
            socket.abort();
        }
        connectLater(RECONNECT_DELAY_SECONDS);
    }

    private void sendJson(int op, Object data) {
        WebSocket socket = webSocket;
        if (socket == null) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("op", op);
            payload.put("d", data);
            socket.sendText(objectMapper.writeValueAsString(payload), true);
        } catch (Exception e) {
            LOG.warn("QQ official bot gateway send failed: {}", e.getMessage());
        }
    }

    private final class GatewayListener implements WebSocket.Listener {

        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket socket) {
            WebSocket.Listener.super.onOpen(socket);
            socket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String payload = textBuffer.toString();
                textBuffer.setLength(0);
                scheduler.execute(() -> handlePayload(payload));
            }
            socket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            stopHeartbeat();
            if (running.get()) {
                LOG.warn("QQ official bot gateway closed status={}, reason={}", statusCode, reason);
                connectLater(RECONNECT_DELAY_SECONDS);
            }
            return null;
        }

        @Override
        public void onError(WebSocket socket, Throwable error) {
            LOG.warn("QQ official bot gateway error: {}", error.getMessage());
            reconnect();
        }
    }
}
