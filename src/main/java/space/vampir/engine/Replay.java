package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.visualization.*;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class Replay {
    public static void main(String[] args) {
        // Map
        final MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_6_vis.svg"),
                147.488,997.344,356.646,
                -100,100,-40,
                47.478824, 19.056313);
//        final MapRender map = new MapRender(RenderExample.class.getResource("/Town10HD.svg"),
//                158.327,683.330,642.063,
//                -100,100,-150,
//                0.0, 0.0);

        JFrame frame = new JFrame();
        SceneVisualization visualization = new SceneVisualization(map);

        // Communication
        StateListener listener = recorder -> {
            var state = recorder.getLastState();
            if(state != null) {
                visualization.show(state);
                SwingUtilities.updateComponentTreeUI(frame);
            }
        };

        StateRecorder recorder = new StateRecorder(listener);
        String url = "ws://localhost:9090";
        OkHttpClient client = new OkHttpClient();
        CountDownLatch latch = new CountDownLatch(1);
        Request request = new Request.Builder().url(url).build();
        var relevantTopics = new HashSet<>(StateRecorder.messageTopics);
        //relevantTopics.addAll(StateRecorder.extraTopics);

        client.newWebSocket(request, new ROSListener(recorder, latch, relevantTopics));

        // Start
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 400));
            frame.setContentPane(new MapPanel(map));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }


}
