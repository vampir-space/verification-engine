package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.VerificationCase;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.verification.VerificationEngineWithRefinery;
import space.vampir.engine.visualization.CliConfig;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.SceneVisualization;
import tools.refinery.mapconverter.map.MapHandler;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class ROSReplayer {
    private static final double DUMMY_VERIFICATION_ENGINE_RADIUS_NOISE = 0.8;
    private static final double DUMMY_VERIFICATION_ENGINE_ANGLE_NOISE = Math.PI / 180;

    public static class NoVerificationEngineRunConfiguration {
        public static void main(String[] args) {
            VerificationEngine verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            CliConfig cliConfig = new CliConfig();
            cliConfig.showStats = false;
            play(verificationEngine, cliConfig);
        }
    }

    public static class DummyVerificationEngineRunConfiguration {
        public static void main(String[] args) {
            VerificationEngine verificationEngine = new DummyVerificationEngine(DUMMY_VERIFICATION_ENGINE_RADIUS_NOISE, DUMMY_VERIFICATION_ENGINE_ANGLE_NOISE);
            play(verificationEngine);
        }
    }

    public static class RefineryVerificationEngineRunConfiguration {
        public static void main(String[] args) throws IOException {
            // TODO remove hardcoded maps in the long run
            final MapRender map = new MapRender("/CrossWalk_6/CrossWalk_6.json");
            final File mapFile = new File(map.getClass().getResource("/CrossWalk_6/Crosswalk_6.xodr").getFile());
            VerificationEngine verificationEngine = new VerificationEngineWithRefinery(new MapHandler(mapFile), map);
            play(verificationEngine);
        }
    }

    public static void main(String[] args) {
        CliConfig cliConfig = CliConfig.get(args);
        VerificationEngine verificationEngine = new DummyVerificationEngine(DUMMY_VERIFICATION_ENGINE_RADIUS_NOISE, DUMMY_VERIFICATION_ENGINE_ANGLE_NOISE);
        play(verificationEngine, cliConfig);
    }

    private static void play(VerificationEngine verificationEngine) {
        play(verificationEngine, new CliConfig());
    }

    private static void play(VerificationEngine verificationEngine, CliConfig cliConfig) {
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
