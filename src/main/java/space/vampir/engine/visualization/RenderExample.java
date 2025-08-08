package space.vampir.engine.visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.net.URL;

import com.github.weisj.jsvg.*;
import com.github.weisj.jsvg.parser.*;
import com.github.weisj.jsvg.view.ViewBox;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RenderExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SVGLoader loader = new SVGLoader();

            URL svgUrl = RenderExample.class.getResource("/CrossWalk_3_objects.svg");
            SVGDocument document = loader.load(Objects.requireNonNull(svgUrl, "SVG file not found"));

            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 400));
            frame.setContentPane(new SVGPanel(Objects.requireNonNull(document)));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static class SVGPanel extends JPanel {
        private final @NotNull SVGDocument map;
        private final SVGDocument car;

        SVGPanel(@NotNull SVGDocument map) {
            this.map = map;

            SVGLoader loader = new SVGLoader();
            car = loader.load(Objects.requireNonNull(SVGPanel.class.getResource("/car.svg"), "SVG file not found"));
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
            map.render(this, g2d, bounds);

            double mapScale = Math.min(
                    getHeight()/map.size().getHeight(),
                    getWidth()/map.size().getWidth());



            g2d.transform(AffineTransform.getTranslateInstance(getWidth()/2,getHeight()/2));
            g2d.transform(AffineTransform.getScaleInstance(mapScale,1/mapScale));
            g2d.transform(AffineTransform.getRotateInstance(0.2));
            g2d.transform(AffineTransform.getTranslateInstance(-car.size().getWidth()/2,-car.size().getHeight()/2));

            car.render(this, g2d);
        }
    }
}