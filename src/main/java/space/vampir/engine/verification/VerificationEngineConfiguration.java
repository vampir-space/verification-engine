package space.vampir.engine.verification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    public double gnssConfidenceRangeMultiplier=1;
    public double yoloRange=50;
    public double yoloAngleOfView=0.01;

    public double yoloMinConfidence = 0.4;
    public double yoloOffsetLongitudinal = 0.0;
    public double yoloOffsetLateral = 0.0;
    public double yoloOffsetAngle = 0.0;
    public double gnssOffsetLongitudinal = 2.39;
    public double gnssOffsetLateral = 0.0;
    public double gtOffsetLongitudinal = 0.0;
    public double gtOffsetLateral = 0.0;

    public VerificationEngineConfiguration() {

    }

    public VerificationEngineConfiguration(String configFilePath) {
        if (configFilePath != null) {
            JsonNode node = getMapConfig(configFilePath);

            relevantMapSegmentSize = node.get("relevantMapSegmentSize").asDouble();
            doRoadCutting = node.get("doRoadCutting").asBoolean();
            roadCutterGranularity = node.get("roadCutterGranularity").asDouble();
            gnssConfidenceRangeMultiplier = node.get("gnssConfidenceRangeMultiplier").asDouble();
            yoloRange = node.get("yoloRange").asDouble();
            yoloAngleOfView = node.get("yoloAngleOfView").asDouble();
            yoloMinConfidence = node.get("yoloMinConfidence").asDouble();
            yoloOffsetLongitudinal =  node.get("yoloOffsetLongitudinal").asDouble();
            yoloOffsetLateral =  node.get("yoloOffsetLateral").asDouble();
            yoloOffsetAngle = node.get("yoloOffsetAngle").asDouble();
            gnssOffsetLongitudinal = node.get("gnssOffsetLongitudinal").asDouble();
            gnssOffsetLateral = node.get("gnssOffsetLateral").asDouble();
            gtOffsetLongitudinal = node.get("gtOffsetLongitudinal").asDouble();
            gtOffsetLateral = node.get("gtOffsetLateral").asDouble();
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
