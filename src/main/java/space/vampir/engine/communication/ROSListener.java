package space.vampir.engine.communication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class ROSListener extends WebSocketListener {

    final StateRecorder stateRecorder;
    final ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    final CountDownLatch latch;

    final Set<String> relevantTopics;
    final Map<String, String> topicMap;
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
        System.out.println("▶ Connected");
    }

    // 2) onMessage: handle either a service_response or publishes
    @Override
    public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
        try {
            Map<?, ?> m = mapper.readValue(text, Map.class);
            String op = (String) m.get("op");
            if ("publish".equals(op)) {
                onMessageReceived(m);
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error parsing incoming message: " + e.getMessage());
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
    public void onClosing(WebSocket ws, int code, String reason) {
        System.out.println("Closing: " + reason);
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
