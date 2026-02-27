package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.RenderExample;
import space.vampir.engine.visualization.SceneVisualization;
import space.vampir.engine.visualization.Visualization;
import space.vampir.engine.visualization.controller.ControlPanel;
import space.vampir.engine.visualization.controller.Controller;
import space.vampir.engine.visualization.controller.KeyBindingManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Replay {

    private static Map<Long, Scenario> states = new LinkedHashMap<>();

    private static class WindowConfig {
        boolean showScene = true;
        boolean showStats = true;
        // add more views later, e.g.:
        // boolean showCamera = false;
        // boolean showRefinery = false;
    }

    private static WindowConfig getWindowConfig(String[] args) {
        WindowConfig config = new WindowConfig();
        for (String arg : args) {
            switch (arg) {
                case "--scene" -> config.showScene = true;
                case "--no-scene" -> config.showScene = false;
                case "--stats" -> config.showStats = true;
                case "--no-stats" -> config.showStats = false;
                // add more options later
            }
        }
        return config;
    }

    public static void main(String[] args) {
        VerificationEngine verificationEngine = new DummyVerificationEngine(0.000025);

        WindowConfig windowConfig = getWindowConfig(args);
        List<Visualization> visualizations = new ArrayList<>();

        // Map
//        final MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_6_vis.svg"),
//                147.488, 997.344, 356.646,
//                -100, 100, -40,
//                47.478824, 19.056313);
        final MapRender map = new MapRender(RenderExample.class.getResource("/Town10HD.svg"),
                158.327, 683.330, 642.063,
                -100, 100, -150,
                0.0, 0.0);

        Controller controller = new Controller();
        ControlPanel controlPanel = new ControlPanel(controller);
        controller.addObserver((time, size) -> {
            var state = states.get(time);
            if (state != null) {
                var updatedScenario = verificationEngine.update(state);
                if (updatedScenario.groundTruth() != null) {
                    for (Visualization visualization : visualizations) {
                        visualization.visualize(state, updatedScenario);
                        visualization.updateWindow();
                    }
                }
            }
        });
        visualizations.add(controlPanel);

        SceneVisualization sceneVisualization = new SceneVisualization(map, windowConfig.showScene);
        visualizations.add(sceneVisualization);

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        VisualStatRepresentation statsVisualization = new VisualStatRepresentation(experimentalEvaluation, windowConfig.showStats, false);
        controller.addObserver(statsVisualization);
        visualizations.add(statsVisualization);

        // Communication
        StateListener listener = recorder -> {
            var state = recorder.getLastState();
            if (state != null) {
                states.put(state.time(), state);
                controller.addTimestampLive(state.time());
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

        // Start windows

        visualizations.forEach(Visualization::startWindow);

        KeyBindingManager keyBindingManager = new KeyBindingManager(controller);
        for (Visualization visualization : visualizations) {
            visualization.registerHotkeys(keyBindingManager);
        }
    }

}
