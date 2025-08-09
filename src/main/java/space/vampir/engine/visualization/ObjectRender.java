package space.vampir.engine.visualization;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.net.URL;
import java.util.Objects;

public class ObjectRender {
    final SVGDocument background;
    double size, x, y, theta;

    /**
     *    x1       x2
     * y +---------+
     */
    public ObjectRender(URL mapURL,
                     double size, double x, double y, double theta) {
        SVGLoader loader = new SVGLoader();
        background = loader.load(Objects.requireNonNull(mapURL, "SVG file not found"));
        this.size = size;
        this.x=x;
        this.y=y;
        this.theta=theta;
    }

    public SVGDocument getBackground() {
        return background;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }
}
