package space.vampir.engine.communication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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

public class ROSListener extends WebSocketListener {
    final String synchronizedURL = "ws://localhost:9091";
    final static boolean USE_ROS_SYNC_NODE = false;

    final StateRecorder stateRecorder;
    final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    final AtomicInteger idCounter = new AtomicInteger(1);
    final CountDownLatch latch;

    final Set<String> relevantTopics;
    final Map<String, String> topicMap;
    final Set<String> seenTopics = ConcurrentHashMap.newKeySet();
    final MessageRecorder messageRecorder;

    public ROSListener(StateRecorder stateRecorder, CountDownLatch latch, Set<String> relevantTopics) {
        this(stateRecorder, latch, relevantTopics, null, null);
    }

    public ROSListener(StateRecorder stateRecorder, CountDownLatch latch, Set<String> relevantTopics, Map<String, String> topicMap, MessageRecorder messageRecorder) {
        this.stateRecorder = stateRecorder;
        this.latch = latch;
        this.relevantTopics = relevantTopics;
        this.topicMap = topicMap;
        this.messageRecorder = messageRecorder;
    }

    // 1) onOpen: start periodic poll
    @Override
    public void onOpen(@NotNull WebSocket ws, @NotNull Response resp) {
        System.out.println("▶ Connected, starting topic polling...");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (this.seenTopics.containsAll(relevantTopics)) {
                System.out.println("▶ All topics discovered, stop polling");
                if (USE_ROS_SYNC_NODE) {
                    System.out.println("▶ Connecting to synchronizer node...");
                    OkHttpClient client = new OkHttpClient();
                    CountDownLatch latch = new CountDownLatch(1);
                    Request request = new Request.Builder().url(synchronizedURL).build();
                    client.newWebSocket(request, new ROSSyncListener(stateRecorder, latch, relevantTopics));
                }
                scheduler.shutdown();
            } else {
//                System.out.println("↻ Polling topics...");
                String reqId = String.valueOf(idCounter.getAndIncrement());
                ROSCallServiceMessage poll = new ROSCallServiceMessage(
                        "call_service", "/rosapi/topics", Map.of(), reqId
                );
                send(ws, poll);
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
                if (!toSubscribe && topicMap != null && topicMap.containsKey(topic)) {
                    var mappedTopic = topicMap.get(topic);
                    toSubscribe = this.relevantTopics.contains(mappedTopic);
                    if (toSubscribe) {
                        seenTopics.add(mappedTopic);
                    }
                }
                System.out.println("\uD83D\uDEC8 New topic: " + topic + ", type: " + type + ", subscribe: " + toSubscribe);
                if (toSubscribe && !USE_ROS_SYNC_NODE) {
                    String subId = String.valueOf(idCounter.getAndIncrement());
                    ROSSubscribeMessage sub = new ROSSubscribeMessage(topic, type, subId);
                    send(ws, sub);
                }
            }
        }
    }

    private void onMessageReceived(Map<?, ?> m) {
        // just print
        String topic = (String) m.get("topic");
        Object msg = m.get("msg");

        // Record message if a recorder is configured
        if (messageRecorder != null) {
            Long timestamp = extractTimestamp(msg);
            messageRecorder.record(topic, msg, timestamp);
        }

        //System.out.println("▶ Message recieved: " + topic);
        try {
            stateRecorder.messageReceived(topic, msg);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to process message on topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract timestamp from ROS message payload if available.
     * Looks for nested header.stamp.sec and header.stamp.nanosec fields.
     */
    private Long extractTimestamp(Object msg) {
        try {
            if (msg instanceof Map<?, ?> m) {
                Object header = m.get("header");
                if (header instanceof Map<?, ?> h) {
                    Object stamp = h.get("stamp");
                    if (stamp instanceof Map<?, ?> s) {
                        Object sec = s.get("sec");
                        Object nano = s.get("nanosec");
                        if (sec instanceof Number && nano instanceof Number) {
                            long secVal = ((Number) sec).longValue();
                            long nanoVal = ((Number) nano).longValue();
                            return secVal * 1000000000L + nanoVal;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore extraction errors
        }
        return null;
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
//            System.out.println("Sending: " + mapper.writeValueAsString(message));
            ws.send(mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing message: " + e.getMessage());
        }
    }
}
