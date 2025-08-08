package space.vampir.engine.communication;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Communication {

    public static void main(String[] args) throws Exception {
        String url = "ws://localhost:9090";
        OkHttpClient client = new OkHttpClient();
        CountDownLatch latch = new CountDownLatch(1);
        Request request = new Request.Builder().url(url).build();

        var relevantTopics = Set.of(
                "/detections/pointpillars",
                "/detections/yolo",
                "/ground_truth/imu",
                "/ground_truth/odometry");

        StateRecorder stateRecorder = new StateRecorder();

        WebSocket ws = client.newWebSocket(request, new ROSListener(stateRecorder, latch, relevantTopics));

        // Block until the socket closes
        latch.await();
        client.dispatcher().executorService().shutdown();
    }
}
