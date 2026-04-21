package space.vampir.engine.visualization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MapProvider {

    public static List<String> getMapConfigs() {
        try (InputStream is = MapProvider.class.getClassLoader().getResourceAsStream("map-list.txt")) {
            if (is == null) {
                System.err.println("Map index file not found in resources.");
                return List.of();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines()
                        .filter(l -> !l.isBlank())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            System.err.println("Error reading map index from resources: " + e.getMessage());
            return List.of();
        }
    }

    public static JsonNode getMapConfig(String path) {
        ObjectMapper mapper = new ObjectMapper();
        String configPath = Objects.requireNonNull(path, "Map config path must not be null");
        if (configPath.isBlank()) {
            throw new IllegalArgumentException("Map config path must not be blank");
        }

        try (InputStream inputStream = openConfigStream(configPath)) {
            return mapper.readTree(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read map config from path: " + configPath, e);
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

        InputStream resourceStream = MapProvider.class.getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream != null) {
            return resourceStream;
        }

        throw new IOException("Map config not found as file or resource: " + path);
    }
}
