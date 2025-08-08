package space.vampir.engine.visualization;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.net.URL;
import java.util.Objects;

public class MapRender {
    final SVGDocument background;
    final double mapXStart;
    final double mapYStart;
    final double background2MapScale;

    /**
     *    x1       x2
     * y +---------+
     */
    public MapRender(URL mapURL,
                     double backgroundX1, double backgroundX2, double backgroundY1,
                     double mapX1, double mapX2, double mapY1) {
        SVGLoader loader = new SVGLoader();
        background = loader.load(Objects.requireNonNull(mapURL, "SVG file not found"));
        background2MapScale = (mapX2-mapX1) / (backgroundX2-backgroundX1);
        mapXStart = mapX1-(backgroundX1*background2MapScale);
        mapYStart = mapY1-(backgroundY1*backgroundY1);
    }
}
