package space.vampir.engine.visualization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import space.vampir.engine.communication.StateRecorder;
import space.vampir.engine.communication.VerificationCaseProvider;
import space.vampir.engine.communication.VerificationCaseProvider.DummyNoiseOdometryProvider;
import space.vampir.engine.communication.VerificationCaseProvider.NavSatOdometryProvider;
import space.vampir.engine.communication.VerificationCaseProvider.RealScenarioProvider;
import space.vampir.engine.communication.scheduler.AlwaysScheduler;
import space.vampir.engine.communication.scheduler.DriveByTopicScheduler;
import space.vampir.engine.communication.scheduler.NewVerificationCaseScheduler;
import space.vampir.engine.communication.synchronizer.ClosestMessageSynchronizer;
import space.vampir.engine.communication.synchronizer.LatestMessageSynchronizer;
import space.vampir.engine.communication.synchronizer.MessageSynchronizer;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.verification.VerificationEngineWithRefinery;
import tools.refinery.mapconverter.map.MapHandler;

import java.io.File;
import java.util.*;

public class CliConfig {
    public boolean showScene = true;
    public boolean showStats = true;
    // add more views later, e.g.:
    // boolean showCamera = false;
    // boolean showRefinery = false;
    public String map = null;
    public String metamodelPath = null;

    // Recording and replay options
    public String recordJsonFile = null;
    public String replayJsonFile = null;

    public long minWaitTime = 1000000000 / 5; // 200ms
    public long maxTimeDifference = 1000000000 / 5; // 200ms
    public long dropOlderThan = 2 * maxTimeDifference;
    public Set<String> relevantTopics = null;
    public Map<String, String> topicMap = Map.of(StateRecorder.groundTruthSimNavSatTopic, StateRecorder.groundTruthGpsTopic, StateRecorder.simNavSatTopic, StateRecorder.lowEndGpsTopic);

    public VerificationEngine verificationEngine = null;
    public VerificationCaseProvider verificationCaseProvider = null;
    public NewVerificationCaseScheduler verificationCaseScheduler = null;
    public MessageSynchronizer messageSynchronizer = null;

    public static CliConfig get(String[] args) {
        CliConfig config = new CliConfig();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--scene" -> config.showScene = true;
                case "--no-scene" -> config.showScene = false;
                case "--stats" -> config.showStats = true;
                case "--no-stats" -> config.showStats = false;
                case "--map" -> {
                    config.map = getValueForFlag(args, i);
                    i++;
                }
                case "--metamodel" -> {
                    config.metamodelPath = getValueForFlag(args, i);
                    i++;
                }
                case "--record-json" -> {
                    config.recordJsonFile = getValueForFlag(args, i);
                    i++;
                }
                case "--replay-json" -> {
                    config.replayJsonFile = getValueForFlag(args, i);
                    i++;
                }
                case "--config" -> {
                    var configFile = getValueForFlag(args, i);
                    if (configFile != null) {
                        i++;
                        ObjectMapper mapper = new ObjectMapper();
                        try {
                            File file = new File(configFile);
                            JsonNode jsonConfig = mapper.readTree(file);

                            if (jsonConfig.has("showScene")) {
                                config.showScene = jsonConfig.get("showScene").asBoolean();
                            }

                            if (jsonConfig.has("showStats")) {
                                config.showStats = jsonConfig.get("showStats").asBoolean();
                            }

                            if (jsonConfig.has("map")) {
                                config.map = jsonConfig.get("map").asText();
                            }

                            if (jsonConfig.has("metamodelPath")) {
                                config.metamodelPath = jsonConfig.get("metamodelPath").asText();
                            }

                            if (jsonConfig.has("recordJsonFile")) {
                                config.recordJsonFile = jsonConfig.get("recordJsonFile").asText();
                            }

                            if (jsonConfig.has("replayJsonFile")) {
                                config.replayJsonFile = jsonConfig.get("replayJsonFile").asText();
                            }

                            if (jsonConfig.has("minWaitTime")) {
                                config.minWaitTime = 1000000L * jsonConfig.get("minWaitTime").asInt();
                            }

                            if (jsonConfig.has("maxTimeDifference")) {
                                config.maxTimeDifference = 1000000L * jsonConfig.get("maxTimeDifference").asInt();
                            }

                            if (jsonConfig.has("dropOlderThan")) {
                                config.dropOlderThan = 1000000L * jsonConfig.get("dropOlderThan").asInt();
                            }

                            if (jsonConfig.has("relevantTopics")) {
                                config.relevantTopics = new HashSet<>();
                                for (JsonNode element : jsonConfig.get("relevantTopics")) {
                                    config.relevantTopics.add(element.asText());
                                }
                            }

                            if (jsonConfig.has("verificationEngine")) {
                                JsonNode subConfig = jsonConfig.get("verificationEngine");
                                String type = subConfig.get("type").asText();
                                config.verificationEngine = switch (type) {
                                    case "None" -> scenario -> new UpdatedScenario(scenario, null,0);
                                    case "DummyVerificationEngine" -> new DummyVerificationEngine(
                                            subConfig.get("radiusStdDev").asDouble(),
                                            getAngle(subConfig, "thetaStdDev")
                                    );
                                    case "VerificationEngineWithRefinery" ->
                                            new VerificationEngineWithRefinery(
                                                    new MapHandler(new File(config.getClass().getResource(subConfig.get("mapFile").asText()).getFile())),
                                                    new MapRender(config.map),
                                                    resolveMetamodelPath(config, subConfig)
                                            );
                                    default ->
                                            throw new IllegalArgumentException("Unknown VerificationEngine type: " + type);
                                };
                            }

                            if (jsonConfig.has("verificationCaseProvider")) {
                                JsonNode subConfig = jsonConfig.get("verificationCaseProvider");
                                String type = subConfig.get("type").asText();
                                config.verificationCaseProvider = switch (type) {
                                    case "DummyNoiseOdometry" -> new DummyNoiseOdometryProvider(
                                            subConfig.get("radiusStdDev").asDouble(),
                                            getAngle(subConfig, "thetaStdDev")
                                    );
                                    case "NavSatOdometry" -> new NavSatOdometryProvider(1,1
                                            //getAngle(subConfig, "thetaStdDev")
                                    );
                                    case "RealScenario" -> new RealScenarioProvider();
                                    default ->
                                            throw new IllegalArgumentException("Unknown VerificationCaseProvider type: " + type);
                                };
                            }

                            if (jsonConfig.has("newVerificationCaseScheduler")) {
                                JsonNode subConfig = jsonConfig.get("newVerificationCaseScheduler");
                                String type = subConfig.get("type").asText();
                                config.verificationCaseScheduler = switch (type) {
                                    case "AlwaysScheduler" -> new AlwaysScheduler();
                                    case "DriveByTopicScheduler" -> new DriveByTopicScheduler(
                                            subConfig.get("topic").asText(),
                                            subConfig.get("delay").asInt()
                                    );
                                    default ->
                                            throw new IllegalArgumentException("Unknown NewVerificationCaseScheduler type: " + type);
                                };
                            }

                            if (jsonConfig.has("messageSynchronizer")) {
                                JsonNode subConfig = jsonConfig.get("messageSynchronizer");
                                List<String> requiredTopics = new ArrayList<>();
                                if (subConfig.has("requiredTopics")) {
                                    for (JsonNode element : subConfig.get("requiredTopics")) {
                                        requiredTopics.add(element.asText());
                                    }
                                }
                                String type = subConfig.get("type").asText();
                                config.messageSynchronizer = switch (type) {
                                    case "ClosestMessageSynchronizer" -> {
                                        Map<String, Integer> priorities = new LinkedHashMap<>();
                                        if (jsonConfig.has("priorities")) {
                                            JsonNode prioritiesNode = jsonConfig.get("priorities");
                                            prioritiesNode.fields().forEachRemaining(entry -> priorities.put(entry.getKey(), entry.getValue().asInt()));
                                        }
                                        yield new ClosestMessageSynchronizer(
                                                config.maxTimeDifference,
                                                requiredTopics,
                                                priorities
                                        );
                                    }
                                    case "LatestMessageSynchronizer" ->
                                            new LatestMessageSynchronizer(
                                                    config.maxTimeDifference,
                                                    requiredTopics
                                            );
                                    default ->
                                            throw new IllegalArgumentException("Unknown MessageSynchronizer type: " + type);
                                };
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to read config file: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return config;
    }

    private static String getValueForFlag(String[] args, int index) {
        if (index + 1 < args.length) {
            return args[index + 1];
        } else {
            System.err.println("Expected value after " + args[index]);
            return null;
        }
    }

    private static String resolveMetamodelPath(CliConfig config, JsonNode verificationEngineConfig) {
        if (verificationEngineConfig.has("metamodelPath")) {
            return verificationEngineConfig.get("metamodelPath").asText();
        }
        return config.metamodelPath;
    }

    private static double getAngle(JsonNode node, String field) {
        String unit = "rad";
        String unitField = field + "Unit";
        if (node.has(unitField)) {
            unit = node.get(unitField).asText();
        }
        double value = node.get(field).asDouble();
        return switch (unit) {
            case "rad" -> value;
            case "deg" -> Math.toRadians(value);
            default -> throw new IllegalArgumentException("Unknown angle unit: " + unit);
        };
    }
}
