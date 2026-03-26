package space.vampir.engine.visualization;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.map.MapObject;
import tools.refinery.mapconverter.map.ObjectType;

import java.io.File;
import java.net.URL;
import java.util.*;

import static com.google.common.io.Resources.getResource;

public class MapRender {
    final SVGDocument background;
    final String mapConfig;
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

        // Extract values
        String urlPath = node.get("mapURL").asText();
        URL mapURL = MapRender.class.getResource(urlPath);

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
        String xodrFilePath = configFilePath.replace(".json", ".xodr");
        URL url = MapRender.class.getResource(xodrFilePath);
        MapHandler mapHandler = null;
        if (url != null) {
            mapHandler = new MapHandler(new File(url.getFile()));
        }

        if (mapHandler != null) {
            LinkedHashMap<Integer, MapObject> objects = mapHandler.getObjects();
            for(MapObject o : objects.values()) {
                //todo theta and size
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
}
