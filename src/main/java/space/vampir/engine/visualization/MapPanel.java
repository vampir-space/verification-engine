package space.vampir.engine.visualization;

import com.github.weisj.jsvg.view.ViewBox;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class MapPanel extends JPanel {
    MapRender map;
    JLabel label = new JLabel();
    JSlider time = new JSlider();

    public MapPanel(MapRender mapRender) {
        this.map = mapRender;

        ToolTipManager.sharedInstance().registerComponent(this);

        this.setLayout(new FlowLayout());

        setLabel();
        label.setVisible(true);
        this.add(label);

        time.setMaximum(1);
        time.setMinimum(0);
        time.setValue(0);
        time.setEnabled(false);

        time.setVisible(true);
        //this.add(time);
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

        for(var object : map.getObjects()) {
            renderObject(object, g2d);
        }
    }

    private void renderObject(ObjectRender object, Graphics2D g2d) {
        // Move to center (in pixels)
        double mapScale = Math.min(
                getWidth() / map.mapXSize,
                getHeight() / map.mapYSize);
        double centerX = map.mapXStart + map.mapXSize/2;
        double xOffset = object.x-centerX;
        double centerY = map.mapYStart + map.mapYSize/2;
        double yOffset = object.y-centerY;

        g2d.transform(AffineTransform.getTranslateInstance(
                getWidth() / 2.0 + xOffset*mapScale,
                getHeight() / 2.0 - yOffset*mapScale));
        // Resize from svg to pixels
        double svgToRenderScale = Math.min(
                getHeight() / map.background.size().height,
                getWidth() / map.background.size().width);
        double objectBackgroundSize = Math.max(
                object.background.size().width,
                object.background.size().height);
        double svgScale = objectBackgroundSize / map.background.size().width;
        double sizeScale = object.size / map.mapXSize;
        double scale = sizeScale*svgToRenderScale/svgScale;
        g2d.transform(AffineTransform.getScaleInstance(scale, scale));
        // Rotate
        g2d.transform(AffineTransform.getRotateInstance(object.theta));
        // move center to 0,0
        g2d.transform(AffineTransform.getTranslateInstance(-object.getBackground().size().getWidth() / 2, -object.getBackground().size().getHeight() / 2));

        object.getBackground().render(this, g2d);
        // Reset
        g2d.setTransform(new AffineTransform());
    }

    public void setLabel() {
        this.label.setText(String.format("%s",map.getName()));
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        double centerX = map.mapXStart + map.mapXSize/2;
        double centerY = map.mapYStart + map.mapYSize/2;

        double scale = Math.max(
                map.mapXSize / getWidth(),
                map.mapYSize / getHeight());
        double posx = centerX + scale*(event.getX()-getWidth()/2.0);
        double posy = centerY - scale*(event.getY()-getHeight()/2.0);

        return String.format("→%.2fm↑%.2fm",posx,posy);
    }
}