package space.vampir.engine.communication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ROSSyncListener extends WebSocketListener {
    final StateRecorder stateRecorder;
    final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    final CountDownLatch latch;

    final Set<String> relevantTopics;

    public ROSSyncListener(StateRecorder stateRecorder, CountDownLatch latch, Set<String> relevantTopics) {
        this.stateRecorder = stateRecorder;
        this.latch = latch;
        this.relevantTopics = relevantTopics;
    }

    @Override
    public void onOpen(@NotNull WebSocket ws, @NotNull Response resp) {
        System.out.println("▶ Connected to synchronizer node.");
        System.out.println("▶ Configuring synchronizer node...");
        Map<String, Object> config = Map.of(
                "topics", relevantTopics,
                "slop", 0.05
        );
        send(ws, config);
    }

    @Override
    public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
        try {
            Map<?, ?> m = mapper.readValue(text, Map.class);
            String op = (String) m.get("op");

            if ("synced_publish".equals(op)) {
                onMessageReceived(m);
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing incoming message: " + e.getMessage());
        }
    }

    private void onMessageReceived(Map<?, ?> m) {
        //System.out.println("▶ Message received: " + m);
        stateRecorder.messageReceived(StateRecorder.syncedTopic, m.get("data"));
    }


    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response resp) {
        System.out.println("⚠ Failed:");
        t.printStackTrace();
        latch.countDown();
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        System.out.println("◼ Disconnected: " + reason);
        latch.countDown();
    }

    private void send(@NotNull WebSocket ws, Object message) {
        try {
            System.out.println("Sending: " + mapper.writeValueAsString(message));
            ws.send(mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing message: " + e.getMessage());
        }
    }
}
