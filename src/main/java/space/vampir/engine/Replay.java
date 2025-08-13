package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.visualization.MapPanel;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.ObjectRender;
import space.vampir.engine.visualization.RenderExample;

import javax.swing.*;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Replay {
    public static void main(String[] args) {
        // Map
        ObjectRender car = new ObjectRender(RenderExample.class.getResource("/car.svg"),
                        5,0,0,3);
        ObjectRender circle = new ObjectRender(RenderExample.class.getResource("/blue-circle.svg"), 30,0,0,90);
        MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_6_vis.svg"),
                293.64313-145.75468,1143.4985-145.75468,522.96765-165.92186,
                -100,100,-40,
                47.478824, 19.056313);
        //map.addObject(car);
        JFrame frame = new JFrame();

        // Communication
        StateListener listener = new StateListener() {
            @Override
            public void stateInvalidated(StateRecorder recorder) {
                var state = recorder.getLastState();
                if(state != null) {
                    var odom = state.odometry();
                    var coord = map.toMapCoord(odom.getX(),odom.getY());

                    car.setX(coord[0]);
                    car.setY(coord[1]);
                    car.setTheta(odom.getTheta());
                    circle.setX(coord[0]);
                    circle.setY(coord[1]);

                    System.out.println(car.getTheta());

                    if(!map.getObjects().contains(car)) {
                        map.addObject(car);
                        map.addObject(circle);
                    }

                    SwingUtilities.updateComponentTreeUI(frame);
                }
            }
        };

        StateRecorder recorder = new StateRecorder(listener);
        String url = "ws://localhost:9090";
        OkHttpClient client = new OkHttpClient();
        CountDownLatch latch = new CountDownLatch(1);
        Request request = new Request.Builder().url(url).build();
        var relevantTopics = Set.of(
//                "/detections/pointpillars",
//                "/detections/yolo",
//                "/ground_truth/imu",
                "/ground_truth/odometry");
        WebSocket ws = client.newWebSocket(request, new ROSListener(recorder, latch, relevantTopics));

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
