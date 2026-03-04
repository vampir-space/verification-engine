package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.communication.VerificationCaseProvider;
import space.vampir.engine.communication.VerificationCaseProvider.DummyNoiseOdometryProvider;
import space.vampir.engine.communication.VerificationCaseProvider.NavSatOdometryProvider;
import space.vampir.engine.communication.VerificationCaseProvider.RealScenarioProvider;
import space.vampir.engine.communication.scheduler.AlwaysScheduler;
import space.vampir.engine.communication.scheduler.DriveByTopicScheduler;
import space.vampir.engine.communication.synchronizer.ClosestMessageSynchronizer;
import space.vampir.engine.communication.synchronizer.LatestMessageSynchronizer;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.verification.VerificationEngineWithRefinery;
import space.vampir.engine.visualization.CliConfig;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.SceneVisualization;
import tools.refinery.mapconverter.map.MapHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class ROSReplayer {

    public static class NoVerificationEngineTestConfiguration {
        public static void main(String[] args) {
            CliConfig cliConfig = new CliConfig();
            cliConfig.verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            cliConfig.showStats = false;
            cliConfig.relevantTopics = Set.of(StateRecorder.odometryTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic, StateRecorder.navSatTopic);
            cliConfig.verificationCaseProvider = new NavSatOdometryProvider(Math.PI / 180);
            cliConfig.verificationCaseScheduler = new AlwaysScheduler();
            cliConfig.messageSynchronizer = new LatestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.odometryTopic, StateRecorder.navSatTopic));
            play(cliConfig);
        }
    }

    public static class NoVerificationEngineDummyNoiseConfiguration {
        public static void main(String[] args) {
            CliConfig cliConfig = new CliConfig();
            cliConfig.verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            cliConfig.showStats = false;
            cliConfig.relevantTopics = Set.of(StateRecorder.odometryTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic);
            cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(2.0, Math.PI / 180);
            cliConfig.verificationCaseScheduler = new AlwaysScheduler();
            cliConfig.messageSynchronizer = new LatestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.odometryTopic));
            play(cliConfig);
        }
    }

    public static class NoVerificationEngineRealConfiguration {
        public static void main(String[] args) {
            CliConfig cliConfig = new CliConfig();
            cliConfig.verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            cliConfig.showStats = false;
            cliConfig.relevantTopics = Set.of(StateRecorder.odometryTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic, StateRecorder.lowEndOdometryTopic);
            cliConfig.verificationCaseProvider = new RealScenarioProvider();
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.lowEndOdometryTopic, (int) cliConfig.maxTimeDifference / 1000000);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.lowEndOdometryTopic, StateRecorder.odometryTopic), Map.of());
            play(cliConfig);
        }
    }

    public static class DummyVerificationEngineTestConfiguration {
        public static void main(String[] args) {
            CliConfig cliConfig = new CliConfig();
            cliConfig.verificationEngine = new DummyVerificationEngine(2.0, Math.PI / 180);
            cliConfig.relevantTopics = Set.of(StateRecorder.odometryTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic);
            cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(4.0, Math.PI / 180);
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.odometryTopic, 0);
            cliConfig.messageSynchronizer = new LatestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.odometryTopic));
            play(cliConfig);
        }
    }

    public static class RefineryVerificationEngineRunConfiguration {
        public static void main(String[] args) throws IOException {
            // TODO remove hardcoded maps in the long run
            final MapRender map = new MapRender("/CrossWalk_6/CrossWalk_6.json");
            final File mapFile = new File(map.getClass().getResource("/CrossWalk_6/Crosswalk_6.xodr").getFile());
            VerificationEngine verificationEngine = new VerificationEngineWithRefinery(new MapHandler(mapFile), map);
            CliConfig cliConfig = new CliConfig();
            cliConfig.verificationEngine = verificationEngine;
            cliConfig.relevantTopics = Set.of(StateRecorder.odometryTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic);
            cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(4.0, Math.PI / 180);
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.odometryTopic, 0);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.odometryTopic, StateRecorder.yoloTopic), Map.of());
            play(cliConfig);
        }
    }

    private static void play(CliConfig cliConfig) {
        StateReplayer stateReplayer = new StateReplayer(cliConfig.verificationEngine);

        MapRender map = MapRender.of(cliConfig.map);
        SceneVisualization sceneVisualization = new SceneVisualization(map, cliConfig.showScene);
        stateReplayer.addVisualization(sceneVisualization);

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        VisualStatRepresentation statsVisualization = new VisualStatRepresentation(experimentalEvaluation, cliConfig.showStats, false);
        stateReplayer.addControllerObserver(statsVisualization);
        stateReplayer.addVisualization(statsVisualization);

        StateListener listener = newState -> {
            if (newState != null) {
                stateReplayer.addState(newState);
            }
        };

        StateRecorder recorder = new StateRecorder(listener,
                cliConfig.verificationCaseProvider,
                cliConfig.verificationCaseScheduler,
                cliConfig.messageSynchronizer,
                cliConfig.minWaitTime,
                cliConfig.dropOlderThan);
//                new DummyNoiseOdometry(1.1, Math.PI / 180));
        String url = "ws://localhost:9090";
        OkHttpClient client = new OkHttpClient();
        CountDownLatch latch = new CountDownLatch(1);
        Request request = new Request.Builder().url(url).build();
        client.newWebSocket(request, new ROSListener(recorder, latch, cliConfig.relevantTopics));

        // Start replayer
        stateReplayer.start();
    }

}
