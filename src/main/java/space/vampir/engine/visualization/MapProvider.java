package space.vampir.engine.visualization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class MapProvider {

    public static List<String> getMapConfigs() {
        try (InputStream is = MapProvider.class.getClassLoader().getResourceAsStream("map-list.txt");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            return reader.lines()
                    .filter(l -> !l.isBlank())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading map index from resources: " + e.getMessage());
            return List.of();
        }
    }

    public static JsonNode getMapConfig(String path) {
        ObjectMapper mapper = new ObjectMapper();
        URL configURL = MapProvider.class.getResource(path);
        try {
            return mapper.readTree(configURL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
