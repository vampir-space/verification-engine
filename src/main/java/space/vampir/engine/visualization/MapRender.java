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

    final double geoX;
    final double geoY;

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

        var mapCoord = transform(geoRefLat,geoRefLon);
        this.geoX = mapCoord[0];
        this.geoY = mapCoord[1];
    }

    public SVGDocument getBackground() {
        return background;
    }

    public String getName() {
        return name;
    }

    private static double[] transform(double lat, double lon) {
        double EARTH_RADIUS_EQUA = 6378137.0;
        double latScale = Math.cos(Math.toRadians(lat));
        double x = latScale * Math.toRadians(lon) * EARTH_RADIUS_EQUA;
        double y = latScale * EARTH_RADIUS_EQUA * lat * Math.log(
                Math.tan(Math.toRadians(90.0 + lat) * Math.PI / 360.0)
        );
        return new double[]{x,y};
    }

    public double[] toMapCoord(double lat, double lon) {
        var r = transform(lat,lon);
        return new double[]{r[0]-this.geoX, this.geoY-r[1]};
    }

    public void addObject(ObjectRender object) {
        objects.add(object);
    }
    public List<ObjectRender> getObjects() {
        return objects;
    }
}
