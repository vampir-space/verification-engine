package space.vampir.engine;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import space.vampir.engine.communication.JsonlMessageRecorder;
import space.vampir.engine.communication.MessageFileReplayer;
import space.vampir.engine.communication.ROSListener;
import space.vampir.engine.communication.StateListener;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.communication.VerificationCaseProvider.DummyNoiseOdometryProvider;
import space.vampir.engine.communication.VerificationCaseProvider.NavSatOdometryProvider;
import space.vampir.engine.communication.VerificationCaseProvider.RealScenarioProvider;
import space.vampir.engine.communication.scheduler.AlwaysScheduler;
import space.vampir.engine.communication.scheduler.DriveByTopicScheduler;
import space.vampir.engine.communication.synchronizer.ClosestMessageSynchronizer;
import space.vampir.engine.communication.synchronizer.LatestMessageSynchronizer;
import space.vampir.engine.verification.*;
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
            String mapPath = extractMapPath(args, "/BME_Town_small/BME_Town_small.json");
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            cliConfig.showStats = false;
            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic, StateRecorder.simNavSatTopic);
            cliConfig.verificationCaseProvider = new NavSatOdometryProvider(1,1);
            cliConfig.verificationCaseScheduler = new AlwaysScheduler();
            cliConfig.messageSynchronizer = new LatestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.simNavSatTopic));
            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    public static class NoVerificationEngineDummyNoiseConfiguration {
        public static void main(String[] args) {
            String mapPath = extractMapPath(args, "/BME_Town_small/BME_Town_small.json");
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            cliConfig.showStats = false;
            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic);
            cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(2.0, Math.PI / 180);
            cliConfig.verificationCaseScheduler = new AlwaysScheduler();
            cliConfig.messageSynchronizer = new LatestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic));
            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    public static class NoVerificationEngineRealConfiguration {
        public static void main(String[] args) {
            String mapPath = extractMapPath(args, "/BME_Town_small/BME_Town_small.json");
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = scenario -> new UpdatedScenario(scenario, null);
            cliConfig.showStats = false;
            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthGpsTopic, StateRecorder.imuTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic, StateRecorder.lowEndGpsTopic);
            cliConfig.verificationCaseProvider = new RealScenarioProvider();
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.lowEndGpsTopic, (int) cliConfig.maxTimeDifference / 1000000);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.lowEndGpsTopic, StateRecorder.groundTruthGpsTopic, StateRecorder.imuTopic), Map.of());
            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    public static class DummyVerificationEngineTestConfiguration {
        public static void main(String[] args) {
            String mapPath = extractMapPath(args, "/BME_Town_small/BME_Town_small.json");
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = new DummyVerificationEngine(2.0, Math.PI / 180);
            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic);
            cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(4.0, Math.PI / 180);
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.groundTruthSimNavSatTopic, 0);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic), Map.of());
            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    public static class RefineryVerificationEngineRunConfiguration {
        public static void main(String[] args) throws IOException {
            String mapPath = extractMapPath(args, "/BME_Town_small/BME_Town_small.json");
            String metamodelPath = extractArgumentValue(args, "--metamodel");
            final MapRender map = MapRender.of(mapPath);
            final File mapFile = new File(map.getXodrURL().getFile());
            VerificationEngine verificationEngine = new VerificationEngineWithRefinery(new MapHandler(mapFile), map, metamodelPath);
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = verificationEngine;
            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.yoloTopic, StateRecorder.simNavSatTopic);
            //cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(4.0, Math.PI / 180);
            cliConfig.verificationCaseProvider = new NavSatOdometryProvider(2,1);
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.groundTruthSimNavSatTopic, 0);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.yoloTopic), Map.of());
            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    public static class RefineryVerificationEngineRealConfiguration {
        public static void main(String[] args) throws IOException {
            String mapPath = extractMapPath(args, "/Krisztina/Krisztina.json");
            String metamodelPath = extractArgumentValue(args, "--metamodel");
            final MapRender map = MapRender.of(mapPath);
            final File mapFile = new File(map.getXodrURL().getFile());
            VerificationEngine verificationEngine = new VerificationEngineWithRefinery(new MapHandler(mapFile), map, metamodelPath);
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = verificationEngine;

            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthGpsTopic, StateRecorder.imuTopic, StateRecorder.yoloTopic, StateRecorder.lowEndGpsTopic);
            //cliConfig.verificationCaseProvider = new DummyNoiseOdometryProvider(4.0, Math.PI / 180);
            cliConfig.verificationCaseProvider = new RealScenarioProvider();
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.lowEndGpsTopic, 0);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.groundTruthGpsTopic, StateRecorder.imuTopic, StateRecorder.lowEndGpsTopic, StateRecorder.yoloTopic), Map.of());

            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    public static class YoloErrorCalculation{
        public static void main(String[] args) throws IOException {
            String mapPath = extractMapPath(args, null);
            String targetId = extractTargetId(args);
            if (mapPath == null || targetId == null) {
                throw new IllegalArgumentException("config file (.json) and targetID are mandatory arguments");
            }
            String metamodelPath = extractArgumentValue(args, "--metamodel");
            final MapRender map = MapRender.of(mapPath);
            final File mapFile = new File(map.getXodrURL().getFile());
            VerificationEngine verificationEngine = new AIErrorCalculator(new MapHandler(mapFile), map, Integer.parseInt(targetId), metamodelPath);
            CliConfig cliConfig = CliConfig.get(args);
            cliConfig.verificationEngine = verificationEngine;
            cliConfig.relevantTopics = Set.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.pointPillarsTopic, StateRecorder.yoloTopic, StateRecorder.simNavSatTopic);
            cliConfig.verificationCaseProvider = new NavSatOdometryProvider(1, 1);
            cliConfig.verificationCaseScheduler = new DriveByTopicScheduler(StateRecorder.groundTruthSimNavSatTopic, 0);
            cliConfig.messageSynchronizer = new ClosestMessageSynchronizer(cliConfig.maxTimeDifference, List.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.imuTopic, StateRecorder.yoloTopic), Map.of());
            cliConfig.map = mapPath;
            play(cliConfig);
        }
    }

    private static void play(CliConfig cliConfig) {
        StateReplayer stateReplayer = new StateReplayer(cliConfig.verificationEngine);

        MapRender map = MapRender.of(cliConfig.map);
        SceneVisualization sceneVisualization = new SceneVisualization(map, cliConfig.showScene);
        stateReplayer.addVisualization(sceneVisualization);

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation(map);
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

        // Start visualizer first (before receiving/replaying messages)
        stateReplayer.start();

        // Check if replay mode or live mode
        if (cliConfig.replayJsonFile != null) {
            replayFromJsonl(recorder, cliConfig.replayJsonFile);
        } else {
            playLiveRos(recorder, cliConfig);
        }
    }

    private static void playLiveRos(StateRecorder recorder, CliConfig cliConfig) {
        // Set up optional message recorder (JSONL)
        JsonlMessageRecorder messageRecorder = null;
        try {
            messageRecorder = cliConfig.recordJsonFile != null ? new JsonlMessageRecorder(new File(cliConfig.recordJsonFile)) : null;
        } catch (IOException e) {
            System.err.println("Error initializing message recorder: " + e.getMessage());
            e.printStackTrace();
        }

        String url = "ws://localhost:9090";
        OkHttpClient client = new OkHttpClient();
        CountDownLatch latch = new CountDownLatch(1);
        Request request = new Request.Builder().url(url).build();

        client.newWebSocket(request, new ROSListener(recorder, latch, cliConfig.relevantTopics, messageRecorder));

        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("[INFO] ROS connection interrupted");
            Thread.currentThread().interrupt();
        } finally {
            if (messageRecorder != null) {
                try {
                    // final close ensures everything flushed and resources released
                    messageRecorder.close();
                    System.out.println("[OK] Recording saved successfully");
                } catch (Exception e) {
                    System.err.println("[ERROR] Error closing recorder: " + e.getMessage());
                }
            }
            client.dispatcher().executorService().shutdownNow();
        }

        System.exit(0);
    }

    private static void replayFromJsonl(StateRecorder recorder, String jsonlPath) {
        File jsonlFile = new File(jsonlPath);
        if (!jsonlFile.exists()) {
            throw new IllegalArgumentException("JSONL file not found: " + jsonlPath);
        }

        MessageFileReplayer replayer = new MessageFileReplayer();
        try {
            replayer.replayJsonl(jsonlFile, recorder::messageReceived);
        } catch (IOException e) {
            throw new RuntimeException("Error replaying from JSONL: " + e.getMessage(), e);
        }
    }

    private static String extractArgumentValue(String[] args, String flag) {
        if (args == null) {
            return null;
        }
        for (int i = 0; i < args.length - 1; i++) {
            if (flag.equals(args[i])) {
                String value = args[i + 1];
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private static String extractMapPath(String[] args, String defaultValue) {
        String mapFlagValue = extractArgumentValue(args, "--map");
        if (mapFlagValue != null) {
            return mapFlagValue;
        }
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank() && !args[0].startsWith("--")) {
            return args[0];
        }
        return defaultValue;
    }

    private static String extractTargetId(String[] args) {
        String targetFlagValue = extractArgumentValue(args, "--target");
        if (targetFlagValue != null) {
            return targetFlagValue;
        }

        if (args == null || args.length == 0) {
            return null;
        }
        int positionalCount = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null || arg.isBlank()) {
                continue;
            }

            if (arg.startsWith("--")) {
                if ("--map".equals(arg) || "--metamodel".equals(arg) || "--target".equals(arg)) {
                    i++;
                }
                continue;
            }

            positionalCount++;
            if (positionalCount == 2) {
                return arg;
            }
        }
        return null;
    }

}
