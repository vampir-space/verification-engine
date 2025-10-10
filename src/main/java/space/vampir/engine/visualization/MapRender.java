package space.vampir.engine.visualization;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapRender {
    final SVGDocument background;
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

    /**
     *    x1       x2
     * y +---------+
     */
    public MapRender(URL mapURL,
                        double backgroundX1, double backgroundX2, double backgroundY1,
                        double mapX1, double mapX2, double mapY1,
                        double geoRefLat, double geoRefLon) {
        SVGLoader loader = new SVGLoader();
        background = Objects.requireNonNull(loader.load(Objects.requireNonNull(mapURL, "SVG file not found")));
        name = mapURL.getFile();
        double background2MapScale = (mapX2-mapX1) / (backgroundX2-backgroundX1);

        mapXStart = mapX1-(backgroundX1-background.viewBox().getMinX())*background2MapScale;
        mapYStart = mapY1-((background.viewBox().getMaxY()-backgroundY1)*background2MapScale);

        mapXSize = (background.viewBox().getMaxX() - background.viewBox().getMinX())*background2MapScale;
        mapYSize = (background.viewBox().getMaxY() - background.viewBox().getMinX())*background2MapScale;

        this.geoRefLatRad = Math.toRadians(geoRefLat);
        this.geoRefLonRad = Math.toRadians(geoRefLon);
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
        return new double[]{x,y};
    }

    public void addObject(ObjectRender object) {
        objects.add(object);
    }
    public List<ObjectRender> getObjects() {
        return objects;
    }
}
