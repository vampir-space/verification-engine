package space.vampir.engine.visualization;

import com.github.weisj.jsvg.view.ViewBox;

import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

class MapPanel extends JPanel {
    MapRender map;
    JLabel label = new JLabel();

    MapPanel(MapRender mapRender) {
        this.map = mapRender;

        ToolTipManager.sharedInstance().registerComponent(this);

        this.add(label);
        label.setVisible(true);
        setLabel();

        final var panel = this;
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                setLabel(e.getX(),e.getY());
                label.invalidate();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                var o = map.getObjects().get(0);
                o.setTheta(o.getTheta()+0.2);
                o.setX(o.x+1);
                panel.updateUI();
            }
        });
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

    public void setLabel(int x, int y) {
        double centerX = map.mapXStart + map.mapXSize/2;
        double centerY = map.mapYStart + map.mapYSize/2;

        double scale = Math.max(
                map.mapXSize / getWidth(),
                map.mapYSize / getHeight());
        double posx = centerX + scale*(x-getWidth()/2.0);
        double posy = centerY - scale*(y-getHeight()/2.0);

        this.setLabel(String.format("→%.2fm↑%.2fm",posx,posy));
    }

    public void setLabel() {
        setLabel("→?m↑?m");
    }

    public void setLabel(String coords) {
        this.label.setText(String.format("%s | C:%s",map.getName(),coords));
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        double centerX = map.mapXStart + map.mapXSize/2;
        double centerY = map.mapYStart + map.mapYSize/2;

        double scale = Math.max(
                map.mapXSize / getWidth(),
                map.mapYSize / getHeight());
        double posx = centerX + scale*(event.getY()-getWidth()/2.0);
        double posy = centerY - scale*(event.getY()-getHeight()/2.0);

        return String.format("→%.2fm↑%.2fm",posx,posy);
    }
}