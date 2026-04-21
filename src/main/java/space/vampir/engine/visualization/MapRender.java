package space.vampir.engine.visualization;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.map.MapObject;
import tools.refinery.mapconverter.map.ObjectType;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

public class MapRender {
    final SVGDocument background;
    final String mapConfig;
    final String xodrPath;
    final URL xodrURL;
    final String name;
    /**
     * The x coordinate of the south-west corner.
     */
    final double mapXStart;
    /**
     * The y coordinate of the south-west corner.
     */
    final double mapYStart;
    final double mapXSize;
    final double mapYSize;

    final double geoRefLatRad;
    final double geoRefLonRad;

    List<ObjectRender> objects = new ArrayList<>();

    //objects on the map (read from the xodr file)
    List<ObjectRender> staticObjects = new ArrayList<>();

    /**
     * x1       x2
     * y +---------+
     */
    public MapRender(URL mapURL,
                     double backgroundX1, double backgroundX2, double backgroundY1,
                     double mapX1, double mapX2, double mapY1,
                     double geoRefLat, double geoRefLon) {
        mapConfig = null;
        xodrPath = null;
        xodrURL = null;

        SVGLoader loader = new SVGLoader();
        background = Objects.requireNonNull(loader.load(Objects.requireNonNull(mapURL, "SVG file not found")));
        name = mapURL.getFile();
        double background2MapScale = (mapX2 - mapX1) / (backgroundX2 - backgroundX1);

        mapXStart = mapX1 - (backgroundX1 - background.viewBox().getMinX()) * background2MapScale;
        mapYStart = mapY1 - ((background.viewBox().getMaxY() - backgroundY1) * background2MapScale);

        mapXSize = (background.viewBox().getMaxX() - background.viewBox().getMinX()) * background2MapScale;
        mapYSize = (background.viewBox().getMaxY() - background.viewBox().getMinX()) * background2MapScale;

        this.geoRefLatRad = Math.toRadians(geoRefLat);
        this.geoRefLonRad = Math.toRadians(geoRefLon);
    }

    public MapRender(String configFilePath) {
        mapConfig = configFilePath;

        // Read JSON file into a tree structure
        JsonNode node = MapProvider.getMapConfig(configFilePath);
        Path configDirectory = resolveConfigDirectory(configFilePath);
        String configResourceDirectory = configDirectory == null ? resolveConfigResourceDirectory(configFilePath) : null;

        // Extract values
        String urlPath = node.get("mapURL").asText();
        URL mapURL = resolveReferenceUrl(urlPath, configDirectory, configResourceDirectory);

        xodrPath = node.get("xodrPath").asText();

        double backgroundX1 = node.get("backgroundX1").asDouble();
        double backgroundX2 = node.get("backgroundX2").asDouble();
        double backgroundY1 = node.get("backgroundY1").asDouble();

        double mapX1 = node.get("mapX1").asDouble();
        double mapX2 = node.get("mapX2").asDouble();
        double mapY1 = node.get("mapY1").asDouble();

        double geoRefLat = node.get("geoRefLat").asDouble();
        double geoRefLon = node.get("geoRefLon").asDouble();

        // Return the initialized class
        SVGLoader loader = new SVGLoader();
        background = Objects.requireNonNull(loader.load(Objects.requireNonNull(mapURL, "SVG file not found")));
        name = mapURL.getFile();
        double background2MapScale = (mapX2 - mapX1) / (backgroundX2 - backgroundX1);

        mapXStart = mapX1 - (backgroundX1 - background.viewBox().getMinX()) * background2MapScale;
        mapYStart = mapY1 - ((background.viewBox().getMaxY() - backgroundY1) * background2MapScale);

        mapXSize = (background.viewBox().getMaxX() - background.viewBox().getMinX()) * background2MapScale;
        mapYSize = (background.viewBox().getMaxY() - background.viewBox().getMinX()) * background2MapScale;

        this.geoRefLatRad = Math.toRadians(geoRefLat);
        this.geoRefLonRad = Math.toRadians(geoRefLon);

        //Adding objects on the map from the xodr file
        xodrURL = resolveReferenceUrl(xodrPath, configDirectory, configResourceDirectory);
        MapHandler mapHandler = null;
        if (xodrURL != null) {
            mapHandler = new MapHandler(new File(xodrURL.getPath()));
        }

        if (mapHandler != null) {
            LinkedHashMap<Integer, MapObject> mapObjects = mapHandler.getObjects();
            for(MapObject o : mapObjects.values()) {
                if(o.getType().equals(ObjectType.Signal)){
                    this.staticObjects.add(new ObjectRender(MapRender.class.getResource("/signal.svg"),"Sign" +o.getId(),4.0, 6.0, o.getCoordinate().getX(), o.getCoordinate().getY(), 0.0));
                }
            }
        }
    }

    public SVGDocument getBackground() {
        return background;
    }

    public String getName() {
        return name;
    }

    public String getXodrPath() {
        return xodrPath;
    }

    public URL getXodrURL() {
        return xodrURL;
    }

    public double[] toMapCoord(double lat, double lon) {
        double EARTH_RADIUS_EQUA = 6378137.0;
        var latRad = Math.toRadians(lat);
        var lonRad = Math.toRadians(lon);
        var dLat = latRad - geoRefLatRad;
        var dLon = lonRad - geoRefLonRad;
        var x = dLon * Math.cos(geoRefLatRad) * EARTH_RADIUS_EQUA;
        var y = dLat * EARTH_RADIUS_EQUA;
        return new double[]{x, y};
    }

    public double[] toGeoCoord(double x, double y) {
        double EARTH_RADIUS_EQUA = 6378137.0;
        var dLat = y / EARTH_RADIUS_EQUA;
        var dLon = x / (Math.cos(geoRefLatRad) * EARTH_RADIUS_EQUA);
        var latRad = geoRefLatRad + dLat;
        var lonRad = geoRefLonRad + dLon;
        var latDeg = Math.toDegrees(latRad);
        var lonDeg = Math.toDegrees(lonRad);
        return new double[]{latDeg, lonDeg};
    }

    public void addObject(ObjectRender object) {
        objects.add(object);
    }

    public void clearObjects() {
        objects.clear();
    }

    public List<ObjectRender> getObjects() {
        return List.copyOf(objects); // copy to avoid concurrent modification issues
    }

    public List<ObjectRender> getStaticObjects(){
        return List.copyOf(staticObjects);
    }

    public static MapRender of(String path) {
        return path == null ? null : new MapRender(path);
    }

    private static Path resolveConfigDirectory(String configFilePath) {
        if (configFilePath == null || configFilePath.isBlank()) {
            return null;
        }

        try {
            Path configPath = Path.of(configFilePath);
            if (!Files.isRegularFile(configPath)) {
                configPath = configPath.toAbsolutePath().normalize();
            }
            if (Files.isRegularFile(configPath)) {
                return configPath.getParent();
            }
        } catch (InvalidPathException ignored) {
            // Classpath config entries may not be valid file-system paths.
        }

        return null;
    }

    private static String resolveConfigResourceDirectory(String configFilePath) {
        if (configFilePath == null || configFilePath.isBlank()) {
            return null;
        }

        String normalized = configFilePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash < 0 ? "" : normalized.substring(0, lastSlash);
    }

    private static URL resolveReferenceUrl(String rawPath, Path configDirectory, String configResourceDirectory) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        return firstResolvedUrl(
                resolveDirectFileUrl(rawPath),
                resolveConfigDirectoryUrl(rawPath, configDirectory),
                resolveConfigResourceDirectoryUrl(rawPath, configResourceDirectory),
                resolveClasspathFallbackUrl(rawPath)
        );
    }

    private static URL firstResolvedUrl(URL... urls) {
        for (URL url : urls) {
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private static URL resolveDirectFileUrl(String rawPath) {
        return toFileUrlIfExists(rawPath);
    }

    private static URL resolveConfigDirectoryUrl(String rawPath, Path configDirectory) {
        if (configDirectory == null) {
            return null;
        }

        String normalized = normalizeRelativePath(rawPath);
        if (normalized.isBlank()) {
            return null;
        }

        return toFileUrlIfExists(configDirectory.resolve(normalized).toString());
    }

    private static URL resolveConfigResourceDirectoryUrl(String rawPath, String configResourceDirectory) {
        if (configResourceDirectory == null) {
            return null;
        }

        String normalized = normalizeRelativePath(rawPath);
        if (normalized.isBlank()) {
            return null;
        }

        return resolveClasspathResource(configResourceDirectory, normalized);
    }

    private static URL resolveClasspathFallbackUrl(String rawPath) {
        String resourcePath = normalizeRelativePath(rawPath);
        URL resourceUrl = MapRender.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            return resourceUrl;
        }

        return MapRender.class.getResource(rawPath.startsWith("/") ? rawPath : "/" + resourcePath);
    }

    private static String normalizeRelativePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static URL resolveClasspathResource(String baseDirectory, String relativePath) {
        String resourcePath = baseDirectory.isBlank() ? relativePath : String.join("/", baseDirectory, relativePath);
        URL resourceUrl = MapRender.class.getClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            return resourceUrl;
        }
        return MapRender.class.getResource("/" + resourcePath);
    }

    private static URL toFileUrlIfExists(String filePath) {
        try {
            Path path = Path.of(filePath);
            if (Files.isRegularFile(path)) {
                return path.toAbsolutePath().normalize().toUri().toURL();
            }
        } catch (InvalidPathException | MalformedURLException ignored) {
            // Invalid file path for this OS or URL conversion issue.
        }
        return null;
    }
}
