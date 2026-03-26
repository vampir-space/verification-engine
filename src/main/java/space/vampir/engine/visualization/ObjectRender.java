package space.vampir.engine.visualization;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.net.URL;
import java.util.Objects;

public class ObjectRender {
    final SVGDocument background;
    double x, y, sizeX, sizeY, theta;
    final String name;

    public ObjectRender(URL mapURL,
                        double sizeX, double sizeY, double x, double y, double theta) {
        this(mapURL,null,sizeX,sizeY,x,y,theta);
    }

    public ObjectRender(URL mapURL, String name,
                     double sizeX, double sizeY, double x, double y, double theta) {
        SVGLoader loader = new SVGLoader();
        background = loader.load(Objects.requireNonNull(mapURL, "SVG file not found"));
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.x=x;
        this.y=y;
        this.theta=theta;
        this.name=name;
    }

    public SVGDocument getBackground() {
        return background;
    }

    public void setSizeX(double sizeX) {
        this.sizeX = sizeX;
    }

    public double getSizeX() {
        return sizeX;
    }

    public void setSizeY(double sizeY) {
        this.sizeY = sizeY;
    }

    public double getSizeY() {
        return sizeY;
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

    public String getName() {
        return name;
    }
}
