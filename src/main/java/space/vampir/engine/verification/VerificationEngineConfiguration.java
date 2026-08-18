package space.vampir.engine.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.ModelSeedFragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class VerificationEngineConfiguration {
    public double relevantMapSegmentSize = 1000;
    public boolean doRoadCutting = false;
    public double roadCutterGranularity = 10;
    public double gnssConfidenceRangeMultiplier = 1;

    public double yoloRange = 20;
    public double yoloAngleOfView = 0.01;
    public double yoloMinConfidence = 0.4;
    public double yoloOffsetLongitudinal = 0.0;
    public double yoloOffsetLateral = 0.0;
    public double yoloOffsetAngle = 0.0;

    public double gnssOffsetLongitudinal = 2.39;
    public double gnssOffsetLateral = 0.0;
    public double gtOffsetLongitudinal = 0.0;
    public double gtOffsetLateral = 0.0;


    // Weights
    double odometryPriorWeight = 0.5;
    double locationDetectionWeight = 1.0;
    double yoloDetectionWeight = 1.0;

    /**
     * Selects the object detection strategy.
     * 1 means Angle based object selection.
     * 2 means distance based object selection.
     */
    public int objectDetectionStrategy = 1;
    ObjectSelection createObjectSelection(Scope<ModelSeedFragment> scope) {
        switch (objectDetectionStrategy){
            case 1: return new AngleBasedObjectSelection(scope);
            case 2: return new DistanceBasedObjectSelection(scope);
            default: throw new IllegalArgumentException("Invalid object detection strategy");
        }
    }
    public VerificationEngineConfiguration() {

    }

    public VerificationEngineConfiguration(String configFilePath) {
        if (configFilePath != null) {
            JsonNode node = getMapConfig(configFilePath);

            relevantMapSegmentSize = node.path("relevantMapSegmentSize").asDouble(relevantMapSegmentSize);
            doRoadCutting = node.path("doRoadCutting").asBoolean(doRoadCutting);
            roadCutterGranularity = node.path("roadCutterGranularity").asDouble(roadCutterGranularity);
            gnssConfidenceRangeMultiplier = node.path("gnssConfidenceRangeMultiplier").asDouble(gnssConfidenceRangeMultiplier);
            yoloRange = node.path("yoloRange").asDouble(yoloRange);
            yoloAngleOfView = node.path("yoloAngleOfView").asDouble(yoloAngleOfView);
            yoloMinConfidence = node.path("yoloMinConfidence").asDouble(yoloMinConfidence);
            yoloOffsetLongitudinal =  node.path("yoloOffsetLongitudinal").asDouble(yoloOffsetLongitudinal);
            yoloOffsetLateral =  node.path("yoloOffsetLateral").asDouble(yoloOffsetLateral);
            yoloOffsetAngle = node.path("yoloOffsetAngle").asDouble(yoloOffsetAngle);
            gnssOffsetLongitudinal = node.path("gnssOffsetLongitudinal").asDouble(gnssOffsetLongitudinal);
            gnssOffsetLateral = node.path("gnssOffsetLateral").asDouble(gnssOffsetLateral);
            gtOffsetLongitudinal = node.path("gtOffsetLongitudinal").asDouble(gtOffsetLongitudinal);
            gtOffsetLateral = node.path("gtOffsetLateral").asDouble(gtOffsetLateral);

            // Weights
            odometryPriorWeight = node.path("odometryPriorWeight").asDouble(0.5);
            locationDetectionWeight = node.path("locationDetectionWeight").asDouble(1.0);
            yoloDetectionWeight = node.path("yoloDetectionWeight").asDouble(1.0);
            objectDetectionStrategy = node.path("objectDetectionStrategy").asInt(objectDetectionStrategy);
        }
    }

    public static JsonNode getMapConfig(String path) {
        ObjectMapper mapper = new ObjectMapper();
        String configPath = Objects.requireNonNull(path, "Verification engine config path must not be null");
        if (configPath.isBlank()) {
            throw new IllegalArgumentException("Verification engine path must not be blank");
        }

        try (InputStream inputStream = openConfigStream(configPath)) {
            return mapper.readTree(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read verification engine config from path: " + configPath, e);
        }
    }

    private static InputStream openConfigStream(String path) throws IOException {
        Path filePath = Path.of(path);
        if (Files.isRegularFile(filePath)) {
            return Files.newInputStream(filePath);
        }

        String resourcePath = path.replace('\\', '/');
        while (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        InputStream resourceStream = VerificationEngineConfiguration.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream != null) {
            return resourceStream;
        }

        throw new IOException("Verification engine config not found as file or resource: " + path);
    }
}
