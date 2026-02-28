package space.vampir.engine.visualization;

import com.github.weisj.jsvg.view.ViewBox;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

public class MapPanel extends JPanel {
    MapRender map;

    public MapPanel(MapRender mapRender) {
        this.map = mapRender;
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setMapRender(MapRender mapRender) {
        this.map = mapRender;
        repaint();
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

        for (var object : map.getObjects()) {
            renderObject(object, g2d);
        }

        for (var object : map.getStaticObjects()) {
            renderObject(object, g2d);
        }
    }

    private void renderObject(ObjectRender object, Graphics2D g2d) {
        AffineTransform old = g2d.getTransform();
        // Move to center to place (in pixels)
        double mapScale = Math.min(
                getWidth() / map.mapXSize,
                getHeight() / map.mapYSize);
        double centerX = map.mapXStart + map.mapXSize / 2;
        double xOffset = object.x - centerX;
        double centerY = map.mapYStart + map.mapYSize / 2;
        double yOffset = object.y - centerY;

        g2d.transform(AffineTransform.getTranslateInstance(
                getWidth() / 2.0 + xOffset * mapScale,
                getHeight() / 2.0 - yOffset * mapScale));

        // Rotate
        g2d.transform(AffineTransform.getRotateInstance(object.theta));

        // Resize from svg to pixels
        double svgToRenderScale = Math.min(
                getHeight() / map.background.size().height,
                getWidth() / map.background.size().width);

        double svgXScale = object.background.size().width / map.background.size().width;
        double svgYScale = object.background.size().height / map.background.size().height;
        double sizeXScale = object.getSizeX() / map.mapXSize;
        double sizeYScale = object.getSizeY() / map.mapYSize;
        double xScale = sizeXScale * svgToRenderScale / svgXScale;
        double yScale = sizeYScale * svgToRenderScale / svgYScale;
        g2d.transform(AffineTransform.getScaleInstance(xScale, yScale));

        // move center to 0,0
        g2d.transform(AffineTransform.getTranslateInstance(-object.getBackground().size().getWidth() / 2, -object.getBackground().size().getHeight() / 2));

        object.getBackground().render(this, g2d);
        // Reset
        g2d.setTransform(old);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        double centerX = map.mapXStart + map.mapXSize / 2;
        double centerY = map.mapYStart + map.mapYSize / 2;

        double scale = Math.max(
                map.mapXSize / getWidth(),
                map.mapYSize / getHeight());
        double posx = centerX + scale * (event.getX() - getWidth() / 2.0);
        double posy = centerY - scale * (event.getY() - getHeight() / 2.0);

        return String.format("→%.2fm↑%.2fm", posx, posy);
    }

    public void saveImage(File file, int width, int height) {
        this.setSize(width, height);
        this.setPreferredSize(new Dimension(width, height));
        BufferedImage bi = new BufferedImage(this.getSize().width, this.getSize().height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        this.doLayout();
        this.paint(g);
        g.dispose();
        try {
            String name = file.getName();
            String format = "";
            int lastIndexOf = name.lastIndexOf(".");
            if (lastIndexOf > -1) {
                format = name.substring(lastIndexOf + 1);
            }
            ImageIO.write(bi, format, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}