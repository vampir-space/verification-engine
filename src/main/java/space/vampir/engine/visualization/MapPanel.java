package space.vampir.engine.visualization;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Objects;

class MapPanel extends JPanel {
    MapRender map;
    JLabel label = new JLabel();
    private final SVGDocument car;

    MapPanel(MapRender mapRender) {
        this.map = mapRender;

        ToolTipManager.sharedInstance().registerComponent(this);
        SVGLoader loader = new SVGLoader();
        car = loader.load(Objects.requireNonNull(MapPanel.class.getResource("/car.svg"), "SVG file not found"));

        this.add(label);
        label.setVisible(true);

        resize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        ViewBox bounds = new ViewBox(0, 0, getWidth(), getHeight());
        map.getBackground().render(this, g2d, bounds);

//        double svgToDrawingScale = svgToRenderScale();

//        g2d.transform(AffineTransform.getTranslateInstance(getWidth() / 2, getHeight() / 2));
//        g2d.transform(AffineTransform.getScaleInstance(svgToDrawingScale, svgToDrawingScale));
//        g2d.transform(AffineTransform.getRotateInstance(0.2));
//        g2d.transform(AffineTransform.getTranslateInstance(-car.size().getWidth() / 2, -car.size().getHeight() / 2));
//
//        car.render(this, g2d);

//        g2d.setTransform(new AffineTransform());
    }

    private double svgToRenderScale() {
        return Math.min(
                getHeight() / map.getBackground().size().getHeight(),
                getWidth() / map.getBackground().size().getWidth());
    }

    public void resize() {
        double centerX = map.mapXStart + map.mapXSize/2;
        double centerY = map.mapYStart + map.mapYSize/2;
        this.label.setText(String.format("%s | C: ↑%.2fm→%.2fm",map.getName(),centerX,centerY));
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        double scale = Math.max(
                map.mapXSize / getWidth(),
                map.mapYSize / getHeight());

        System.out.println(scale);

        double centerX = map.mapXStart + map.mapXSize/2;
        double centerY = map.mapYStart + map.mapYSize/2;

//        double x = map.mapXStart + event.getX() / scale;
//        double y = map.mapYStart + map.mapYSize - map.mapYSize*((event.getY()+0.0)/getHeight());

        double x = centerX + scale*(event.getX()-getWidth()/2.0);
        double y = centerY - scale*(event.getY()-getHeight()/2.0);

        return String.format("%.2fm/%.2fm",x,y);
    }
}