package space.vampir.engine.geometry;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/** Java2D visualizer for GeometrySolver.
 *  Colors: Odometry=RED, Points=BLUE, Rays+Origins=GREEN, Solution=BLACK.
 */
public class GeometryVisualizer {

    public static void render(GeometrySolver solver,
                              String filename,
                              int width, int height,
                              double xMin, double xMax,
                              double yMin, double yMax,
                              int maxIter) {

        // 1) Solve (without mutating constraints)
        double[] sol = solver.solve(maxIter);
        double sx = sol[0], sy = sol[1];

        // 2) Pull read-only data
        List<GeometrySolver.PointView> points = solver.getPoints();
        List<GeometrySolver.RayView>   rays   = solver.getRays();
        GeometrySolver.PointView odo = solver.getOdometry();

        // 3) Canvas
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // 4) World->screen transform
        final double W = width, H = height;
        final double wx = xMax - xMin, wy = yMax - yMin;
        java.util.function.DoubleUnaryOperator X = (x) -> (x - xMin) / wx * W;
        java.util.function.DoubleUnaryOperator Y = (y) -> H - (y - yMin) / wy * H;

        // 5) Grid + axes
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(230,230,230));
        double gridStep = niceStep(wx, 10);
        for (double x = Math.ceil(xMin / gridStep) * gridStep; x <= xMax + 1e-9; x += gridStep) {
            int sxp = (int)Math.round(X.applyAsDouble(x));
            g.drawLine(sxp, 0, sxp, height);
        }
        for (double y = Math.ceil(yMin / gridStep) * gridStep; y <= yMax + 1e-9; y += gridStep) {
            int syp = (int)Math.round(Y.applyAsDouble(y));
            g.drawLine(0, syp, width, syp);
        }
        g.setColor(new Color(150,150,150));
        if (yMin <= 0 && yMax >= 0) {
            int ya = (int)Math.round(Y.applyAsDouble(0));
            g.drawLine(0, ya, width, ya);
        }
        if (xMin <= 0 && xMax >= 0) {
            int xa = (int)Math.round(X.applyAsDouble(0));
            g.drawLine(xa, 0, xa, height);
        }

        // 6) Rays (GREEN) + origins
        g.setStroke(new BasicStroke(2f));
        Color green = new Color(0,140,0);
        g.setColor(green);
        for (GeometrySolver.RayView r : rays) {
            double[] end = rayBoxIntersection(r.px, r.py, r.vx, r.vy, xMin, xMax, yMin, yMax);
            int x0 = (int)Math.round(X.applyAsDouble(r.px));
            int y0 = (int)Math.round(Y.applyAsDouble(r.py));
            int x1 = (int)Math.round(X.applyAsDouble(end[0]));
            int y1 = (int)Math.round(Y.applyAsDouble(end[1]));
            g.drawLine(x0, y0, x1, y1);
            fillDot(g, x0, y0, 6, green);
        }

        // 7) Points (BLUE)
        Color blue = new Color(0,90,200);
        for (GeometrySolver.PointView p : points) {
            int xp = (int)Math.round(X.applyAsDouble(p.x));
            int yp = (int)Math.round(Y.applyAsDouble(p.y));
            fillDot(g, xp, yp, 6, blue);
        }

        // 8) Odometry (RED)
        if (odo != null) {
            int xo = (int)Math.round(X.applyAsDouble(odo.x));
            int yo = (int)Math.round(Y.applyAsDouble(odo.y));
            fillDot(g, xo, yo, 7, new Color(200,0,0));
        }

        // 9) Solution (BLACK)
        int xs = (int)Math.round(X.applyAsDouble(sx));
        int ys = (int)Math.round(Y.applyAsDouble(sy));
        fillDot(g, xs, ys, 7, Color.BLACK);

        // Label
        g.setColor(Color.DARK_GRAY);
        g.drawString(String.format("Solution (%.4f, %.4f)", sx, sy), 10, 20);

        g.dispose();
        try {
            ImageIO.write(img, "PNG", new File(filename));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save visualization: " + e.getMessage(), e);
        }
    }

    private static void fillDot(Graphics2D g, int x, int y, int r, Color c) {
        Color old = g.getColor();
        g.setColor(c);
        g.fillOval(x - r, y - r, 2*r, 2*r);
        g.setColor(old);
    }

    /** Extend ray to first hit with bounds (t >= 0), else far fallback. */
    private static double[] rayBoxIntersection(double px, double py, double vx, double vy,
                                               double xMin, double xMax, double yMin, double yMax) {
        double bestT = Double.POSITIVE_INFINITY;
        double bx = px, by = py;
        if (Math.abs(vx) > 1e-12) {
            double t = (xMax - px)/vx;
            if (t >= 0) {
                double y = py + t*vy;
                if (y >= yMin - 1e-12 && y <= yMax + 1e-12 && t < bestT) { bestT = t; bx = xMax; by = y; }
            }
            t = (xMin - px)/vx;
            if (t >= 0) {
                double y = py + t*vy;
                if (y >= yMin - 1e-12 && y <= yMax + 1e-12 && t < bestT) { bestT = t; bx = xMin; by = y; }
            }
        }
        if (Math.abs(vy) > 1e-12) {
            double t = (yMax - py)/vy;
            if (t >= 0) {
                double x = px + t*vx;
                if (x >= xMin - 1e-12 && x <= xMax + 1e-12 && t < bestT) { bestT = t; bx = x; by = yMax; }
            }
            t = (yMin - py)/vy;
            if (t >= 0) {
                double x = px + t*vx;
                if (x >= xMin - 1e-12 && x <= xMax + 1e-12 && t < bestT) { bestT = t; bx = x; by = yMin; }
            }
        }
        if (!Double.isFinite(bestT)) return new double[]{px + 1e3*vx, py + 1e3*vy};
        return new double[]{bx, by};
    }

    /** Choose a “nice” grid step for ~10 lines. */
    private static double niceStep(double span, int targetLines) {
        double raw = span / Math.max(2, targetLines);
        double exp = Math.floor(Math.log10(raw));
        double base = Math.pow(10, exp);
        double m = raw / base;
        if (m < 1.5) return 1 * base;
        if (m < 3.5) return 2 * base;
        if (m < 7.5) return 5 * base;
        return 10 * base;
    }
}
