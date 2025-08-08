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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ROSListener extends WebSocketListener {
    final StateRecorder stateRecorder;
    final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    final AtomicInteger idCounter = new AtomicInteger(1);
    final CountDownLatch latch;

    final Set<String> relevantTopics;
    final Set<String> seenTopics = ConcurrentHashMap.newKeySet();

    ROSListener(StateRecorder stateRecorder, CountDownLatch latch, Set<String> relevantTopics) {
        this.stateRecorder = stateRecorder;
        this.latch = latch;
        this.relevantTopics = relevantTopics;
    }

    // 1) onOpen: start periodic poll
    @Override
    public void onOpen(@NotNull WebSocket ws, @NotNull Response resp) {
        System.out.println("▶ Connected, starting topic polling...");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if(this.seenTopics.containsAll(relevantTopics)) {
                System.out.println("▶ All topics discovered, stop polling");
                scheduler.shutdown();
            } else {
                System.out.println("↻ Polling topics...");
                String reqId = String.valueOf(idCounter.getAndIncrement());
                ROSCallServiceMessage poll = new ROSCallServiceMessage(
                        "call_service", "/rosapi/topics", Map.of(), reqId
                );
                try {
                    ws.send(mapper.writeValueAsString(poll));
                } catch (JsonProcessingException e) {
                    System.err.println("Error serializing poll message: " + e.getMessage());
                }
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    // 2) onMessage: handle either a service_response or publishes
    @Override
    public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
        try {
            Map<?, ?> m = mapper.readValue(text, Map.class);
            String op = (String) m.get("op");

            if ("service_response".equals(op) && "/rosapi/topics".equals(m.get("service"))) {
                onTopicsAnnounced(ws, m);
            } else if ("publish".equals(op)) {
                onMessageReceived(m);
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing incoming message: " + e.getMessage());
        }
    }

    private void onTopicsAnnounced(@NotNull WebSocket ws, Map<?, ?> m) {
        Map<?, ?> vals = (Map<?, ?>) m.get("values");
        List<String> topics = (List<String>) vals.get("topics");
        List<String> types = (List<String>) vals.get("types");

        for (int i = 0; i < topics.size(); i++) {
            String topic = topics.get(i);
            String type = types.get(i);
            if (seenTopics.add(topic)) {
                boolean toSubscribe = this.relevantTopics.contains(topic);
                System.out.println("\uD83D\uDEC8 New topic: " + topic + ", subscribe: " + toSubscribe);
                if(toSubscribe){
                    // new topic → subscribe
                    String subId = String.valueOf(idCounter.getAndIncrement());
                    ROSSubscribeMessage sub = new ROSSubscribeMessage(
                            "subscribe", topic, type, subId
                    );
                    try {
                        ws.send(mapper.writeValueAsString(sub));
                        System.out.println("▶ Subscribed to new topic: " + topic);
                    } catch (JsonProcessingException e) {
                        System.err.println("Error serializing subscribe message: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void onMessageReceived(Map<?, ?> m) {
        // just print
        String topic = (String) m.get("topic");
        Object msg = m.get("msg");
        System.out.println("▶ Message recieved: " + topic);
        stateRecorder.messageReceived(topic,msg);
//        try {
//            System.out.printf("▶ [%s] %s%n", topic, mapper.writeValueAsString(msg));
//        } catch (JsonProcessingException e) {
//            System.out.printf("▶ [%s] <error serializing message: %s>%n", topic, e.getMessage());
//        }
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
}
