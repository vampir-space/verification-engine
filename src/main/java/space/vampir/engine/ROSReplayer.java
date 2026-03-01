package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.VerificationCase;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.visualization.CliConfig;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.SceneVisualization;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class ROSReplayer {
    private static final double DUMMY_VERIFICATION_ENGINE_NOISE = 0.8;

    public static void main(String[] args) {
        CliConfig cliConfig = CliConfig.get(args);
        VerificationEngine verificationEngine = new DummyVerificationEngine(DUMMY_VERIFICATION_ENGINE_NOISE);
        StateReplayer stateReplayer = new StateReplayer(verificationEngine);

        MapRender map = MapRender.of(cliConfig.map);
        SceneVisualization sceneVisualization = new SceneVisualization(map, cliConfig.showScene);
        stateReplayer.addVisualization(sceneVisualization);

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        VisualStatRepresentation statsVisualization = new VisualStatRepresentation(experimentalEvaluation, cliConfig.showStats, false);
        stateReplayer.addControllerObserver(statsVisualization);
        stateReplayer.addVisualization(statsVisualization);

        // Communication
        StateListener listener = recorder -> {
            VerificationCase state = recorder.getLastState();
            if (state != null && state.scenario().odometry() != null) {
                stateReplayer.addState(state);
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
