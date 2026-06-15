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
    private double zoomFactor = 1.0;
    private final double ZOOM_MIN = 1.0;
    private final double ZOOM_MAX = 12.0;

    // Values computed per-paint for consistent mapping when rendering objects
    private double currentMapScale = 1.0; // pixels per map-meter
    private double currentCenterX = 0.0; // map coord
    private double currentCenterY = 0.0; // map coord

    // Background caching for performance optimization
    // Cache stores the rasterized background at the map scale (full map extent at current zoom)
    private BufferedImage cachedBackgroundImage;
    private double cachedZoomFactor = -1;
    private Object cachedBackgroundDocument = null;
    private double cachedMapXSize = -1;
    private double cachedMapYSize = -1;

    public MapPanel(MapRender mapRender) {
        this.map = mapRender;
        ToolTipManager.sharedInstance().registerComponent(this);
        // mouse wheel zoom
        this.addMouseWheelListener(e -> {
            int notches = e.getWheelRotation();
            if (notches < 0) {
                // zoom in
                zoomFactor = Math.min(ZOOM_MAX, zoomFactor * Math.pow(1.125, -notches));
            } else if (notches > 0) {
                // zoom out
                zoomFactor = Math.max(ZOOM_MIN, zoomFactor / Math.pow(1.125, notches));
            }
            repaint();
        });
    }

    public void setMapRender(MapRender mapRender) {
        this.map = mapRender;
        // Invalidate cache when map changes
        invalidateBackgroundCache();
        repaint();
    }

    /**
     * Invalidates the cached background image to force re-rendering on next paint.
     */
    private void invalidateBackgroundCache() {
        cachedBackgroundImage = null;
        cachedZoomFactor = -1;
        cachedBackgroundDocument = null;
        cachedMapXSize = -1;
        cachedMapYSize = -1;
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

        // Determine base scale (fits map fully into panel) and apply zoom
        double baseScale = Math.min(
                getWidth() / map.mapXSize,
                getHeight() / map.mapYSize);
        currentMapScale = baseScale * zoomFactor;

        // Determine center of view. When zoomed out (zoomFactor == ZOOM_MIN) keep map center,
        // otherwise center on the object named "ve" if present.
        if (Math.abs(zoomFactor - ZOOM_MIN) < 1e-9) {
            currentCenterX = map.mapXStart + map.mapXSize / 2.0;
            currentCenterY = map.mapYStart + map.mapYSize / 2.0;
        } else {
            ObjectRender focus = null;
            for (var o : map.getObjects()) {
                if (o.getName() != null && o.getName().equals("ego")) {
                    focus = o;
                }
                if (o.getName() != null && o.getName().equals("gt")) {
                    focus = o;
                    break;
                }
            }
            if (focus != null) {
                currentCenterX = focus.getX();
                currentCenterY = focus.getY();
            } else {
                currentCenterX = map.mapXStart + map.mapXSize / 2.0;
                currentCenterY = map.mapYStart + map.mapYSize / 2.0;
            }
        }

        // Draw the SVG background directly, using transforms so it lines up with object coordinates
        try {
            var bg = map.getBackground();
            double bw = bg.size().width;
            double bh = bg.size().height;
            if (bw > 0 && bh > 0) {
                double tx = getWidth() / 2.0 + (map.mapXStart - currentCenterX) * currentMapScale;
                double ty = getHeight() / 2.0 - (map.mapYStart + map.mapYSize - currentCenterY) * currentMapScale;

                // We rasterize once at the maximum zoom (ZOOM_MAX) using the current base scale,
                // but limit raster size to avoid OOM. Later we will scale this raster down for lower zooms.
                final int MAX_RASTER_DIM = 10000; // max width/height in pixels for rasterized image

                // Compute baseScale (pixels per map-meter without zoom) - already computed above
                // Desired raster scale (pixels per map-meter) at maximum zoom
                double desiredRasterScale = baseScale * ZOOM_MAX;

                // Desired image size for full map at max zoom
                double desiredImgW = map.mapXSize * desiredRasterScale;
                double desiredImgH = map.mapYSize * desiredRasterScale;

                double rasterScaleToUse = desiredRasterScale;
                if (desiredImgW > MAX_RASTER_DIM || desiredImgH > MAX_RASTER_DIM) {
                    // Reduce raster scale so dimensions fit within cap
                    double downscaleFactor = Math.min(MAX_RASTER_DIM / desiredImgW, MAX_RASTER_DIM / desiredImgH);
                    rasterScaleToUse = desiredRasterScale * downscaleFactor;
                }

                // Cache is valid if we already rasterized the same background with the same rasterScale
                boolean cacheValid = cachedBackgroundImage != null
                        && cachedBackgroundDocument == bg
                        && Math.abs(cachedMapXSize - map.mapXSize) < 1e-9
                        && Math.abs(cachedMapYSize - map.mapYSize) < 1e-9
                        && Math.abs(cachedZoomFactor - rasterScaleToUse) < 1e-9;

                if (!cacheValid) {
                    int imgWidth = (int) Math.max(1, Math.round(map.mapXSize * rasterScaleToUse));
                    int imgHeight = (int) Math.max(1, Math.round(map.mapYSize * rasterScaleToUse));

                    cachedBackgroundImage = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D cachedG2d = cachedBackgroundImage.createGraphics();
                    cachedG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    cachedG2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                    // We need to compute the scale to render the SVG into our raster image.
                    // SVG native size is bw x bh. We want map.mapXSize meters to map to imgWidth pixels.
                    double sxRaster = imgWidth / Math.max(1.0, bw);
                    double syRaster = imgHeight / Math.max(1.0, bh);

                    AffineTransform cachedTransform = AffineTransform.getScaleInstance(sxRaster, syRaster);
                    cachedG2d.transform(cachedTransform);
                    ViewBox bounds = new ViewBox(0, 0, (int) Math.max(1, bw), (int) Math.max(1, bh));
                    bg.render(this, cachedG2d, bounds);
                    cachedG2d.dispose();

                    cachedZoomFactor = rasterScaleToUse; // store raster pixels-per-meter as key
                    cachedBackgroundDocument = bg;
                    cachedMapXSize = map.mapXSize;
                    cachedMapYSize = map.mapYSize;
                }

                // Draw the cached raster. The cached raster uses pixels-per-meter = cachedZoomFactor;
                // currentMapScale is pixels-per-meter for current view. Compute scale factor to apply to raster.
                double cachedRasterScale = cachedZoomFactor; // pixels per map-meter
                double scaleFactor = currentMapScale / cachedRasterScale;

                AffineTransform old = g2d.getTransform();
                AffineTransform drawTransform = new AffineTransform();
                drawTransform.translate(tx, ty);
                drawTransform.scale(scaleFactor, scaleFactor);
                g2d.transform(drawTransform);
                g2d.drawImage(cachedBackgroundImage, 0, 0, this);
                g2d.setTransform(old);
            } else {
                // Fallback: render SVG directly to fill panel (original behavior)
                ViewBox bounds = new ViewBox(0, 0, getWidth(), getHeight());
                bg.render(this, g2d, bounds);
                // Invalidate cache for fallback renders
                invalidateBackgroundCache();
            }
        } catch (Exception ex) {
            // don't fail painting on background render issues
            ex.printStackTrace();
            ViewBox bounds = new ViewBox(0, 0, getWidth(), getHeight());
            map.getBackground().render(this, g2d, bounds);
            // Invalidate cache on error
            invalidateBackgroundCache();
        }

        for (var object : map.getObjects()) {
            renderObject(object, g2d);
        }

        for (var object : map.getStaticObjects()) {
            renderObject(object, g2d);
        }
    }

    private void renderObject(ObjectRender object, Graphics2D g2d) {
        AffineTransform old = g2d.getTransform();

        // Move to center to place (in pixels) using precomputed scale/center
        double xOffset = object.x - currentCenterX;
        double yOffset = object.y - currentCenterY;

        g2d.transform(AffineTransform.getTranslateInstance(
                getWidth() / 2.0 + xOffset * currentMapScale,
                getHeight() / 2.0 - yOffset * currentMapScale));

        // Rotate
        g2d.transform(AffineTransform.getRotateInstance(object.theta));

        // Resize so the object's rendered pixel size equals object.getSizeX()*currentMapScale
        double objBgW = object.getBackground().size().getWidth();
        double objBgH = object.getBackground().size().getHeight();
        double xScale = (object.getSizeX() * currentMapScale) / objBgW;
        double yScale = (object.getSizeY() * currentMapScale) / objBgH;
        g2d.transform(AffineTransform.getScaleInstance(xScale, yScale));

        // move center to 0,0
        g2d.transform(AffineTransform.getTranslateInstance(-object.getBackground().size().getWidth() / 2, -object.getBackground().size().getHeight() / 2));

        object.getBackground().render(this, g2d);
        // Reset
        g2d.setTransform(old);


        if(object.getName() != null) {
            int textX = (int) (getWidth() / 2.0 + xOffset * currentMapScale);
            int textY = (int) (getHeight() / 2.0 - yOffset * currentMapScale);

            int fontSize = Math.max(8, (int) (currentMapScale*object.getSizeY()/4));
            int textOffset = (int) (currentMapScale*object.getSizeX()/4);

            var font = new Font(g2d.getFont().getName(),Font.PLAIN,fontSize);
            g2d.setFont(font);
            g2d.drawString(object.getName(), textX+textOffset, textY+fontSize/2);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        // Use the same mapping as used for rendering
        double centerX = currentCenterX == 0.0 ? map.mapXStart + map.mapXSize / 2.0 : currentCenterX;
        double centerY = currentCenterY == 0.0 ? map.mapYStart + map.mapYSize / 2.0 : currentCenterY;
        double mapScale = currentMapScale == 0.0 ? Math.min(getWidth() / map.mapXSize, getHeight() / map.mapYSize) : currentMapScale;

        double posx = centerX + (event.getX() - getWidth() / 2.0) / mapScale;
        double posy = centerY - (event.getY() - getHeight() / 2.0) / mapScale;

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