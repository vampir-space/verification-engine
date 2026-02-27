package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.RenderExample;
import space.vampir.engine.visualization.SceneVisualization;
import space.vampir.engine.visualization.WindowConfig;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class ROSReplayer {

    private static final double DUMMY_GNSS_NOISE = 1.1;
    private static final double DUMMY_VERIFICATION_ENGINE_NOISE = 0.8;

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
        WindowConfig windowConfig = getWindowConfig(args);
        VerificationEngine verificationEngine = new DummyVerificationEngine(DUMMY_VERIFICATION_ENGINE_NOISE);

        // Map
//        final MapRender map = new MapRender("/CrossWalk_6/CrossWalk_6.json");
        final MapRender map = new MapRender("/Town10HD/Town10HD.json");

        StateReplayer stateReplayer = new StateReplayer(verificationEngine);

        SceneVisualization sceneVisualization = new SceneVisualization(map, windowConfig.showScene);
        stateReplayer.addVisualization(sceneVisualization);

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        VisualStatRepresentation statsVisualization = new VisualStatRepresentation(experimentalEvaluation, windowConfig.showStats, false);
        stateReplayer.addControllerObserver(statsVisualization);
        stateReplayer.addVisualization(statsVisualization);

        // Communication
        StateListener listener = recorder -> {
            Scenario state = recorder.getLastState();
            if (state != null) {
                UpdatedScenario updatedScenario = new UpdatedScenario(
                        new Scenario(
                                state.time(),
                                NoiseApplier.addNoise(state.odometry(), DUMMY_GNSS_NOISE),
                                state.pointPillars(),
                                state.yolo()
                        ),
                        null,
                        state.odometry()
                );
                stateReplayer.addState(updatedScenario);
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

        // Start replayer
        stateReplayer.start();
    }

}
