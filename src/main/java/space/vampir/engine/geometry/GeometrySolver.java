package space.vampir.engine.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Minimize sum of squared distances from Points and Rays.
 *  Non-negativity on rays enforced via active-set.
 *  Adds a single Odometry point with weight c, and scales all other terms by (1-c).
 */
public class GeometrySolver {
    private final List<Point> points = new ArrayList<>();
    private final List<Ray> rays = new ArrayList<>();

    // Optional odometry term
    private Point odometry = null;
    private double c = 0.0; // weight on odometry term in [0,1]

    public void clear() {
        rays.clear();
        points.clear();
        odometry = null;
        c = 0.0;
    }

    /** Add a regular point (location) constraint (goes into the (1-c) group). */
    public void addPoint(double x, double y) {
        points.add(new Point(x, y));
    }

    /** Add a ray constraint (direction auto-normalized). */
    public void addRay(double px, double py, double vx, double vy) {
        rays.add(new Ray(px, py, vx, vy));
    }

    /** Set (or replace) the odometry point and its weight c in [0,1]. */
    public void setOdometry(double x, double y, double c) {
        if (Double.isNaN(c) || c < 0.0 || c > 1.0)
            throw new IllegalArgumentException("Weight c must be in [0,1]");
        this.odometry = new Point(x, y);
        this.c = c;
    }

    /**
     * Solve for (x,y) using active-set least squares for rays (t>=0) + point constraints,
     * with odometry weighted by c and all other terms scaled by (1-c).
     * @param maxIter maximum active-set iterations (e.g. 10)
     * @return [x,y] solution
     */
    public double[] solve(int maxIter) {
        double x = 0.0, y = 0.0;

        // All rays are active (treat as infinite lines) initially
        boolean[] active = new boolean[rays.size()];
        Arrays.fill(active, true);

        // First mixed solve using initial active set
        double[] xy = solveMixed(active);
        x = xy[0]; y = xy[1];

        for (int it = 0; it < maxIter; it++) {
            boolean changed = false;
            // Update activity based on current (x,y)
            for (int i = 0; i < rays.size(); i++) {
                Ray r = rays.get(i);
                double t = r.vx * (x - r.px) + r.vy * (y - r.py);
                boolean shouldBeActive = (t >= 0); // Projection falls on the ray
                if (shouldBeActive != active[i]) { active[i] = shouldBeActive; changed = true; }
            }
            if (!changed) break; // Active set stabilized
            xy = solveMixed(active);
            x = xy[0]; y = xy[1];
        }
        return new double[]{x, y};
    }

    // Build & solve the 2x2 normal equations for the current active set.
    private double[] solveMixed(boolean[] active) {
        double a11 = 0, a12 = 0, a22 = 0; // Symmetric matrix M
        double b1  = 0, b2  = 0;          // Right-hand side b

        // Weights
        final double wOdo  = (odometry != null ? c : 0.0);
        final double wRest = 1.0 - wOdo;  // scales every non-odometry term

        // Rays (scaled by wRest)
        for (int i = 0; i < rays.size(); i++) {
            Ray r = rays.get(i);
            if (active[i]) {
                // Line projector A = I - v v^T
                double vv11 = r.vx * r.vx;
                double vv12 = r.vx * r.vy;
                double vv22 = r.vy * r.vy;

                double a11i = wRest * (1 - vv11);
                double a12i = wRest * (-vv12);
                double a22i = wRest * (1 - vv22);

                a11 += a11i; a12 += a12i; a22 += a22i;
                b1  += a11i * r.px + a12i * r.py;
                b2  += a12i * r.px + a22i * r.py;
            } else {
                // Blocked ray behaves like a point at (px,py)
                a11 += wRest; a22 += wRest;
                b1  += wRest * r.px; b2 += wRest * r.py;
            }
        }

        // Regular points (scaled by wRest)
        for (Point p : points) {
            a11 += wRest; a22 += wRest;
            b1  += wRest * p.x; b2 += wRest * p.y;
        }

        // Odometry point (own weight c)
        if (odometry != null && wOdo > 0) {
            a11 += wOdo; a22 += wOdo;
            b1  += wOdo * odometry.x; b2 += wOdo * odometry.y;
        }

        // Small regularization to avoid singularity in degenerate cases
        double reg = 1e-12;
        a11 += reg; a22 += reg;

        // Solve 2x2 system M x = b
        double det = a11 * a22 - a12 * a12;
        if (Math.abs(det) < 1e-18) {
            double boost = 1e-8 * (Math.abs(a11) + Math.abs(a22) + 1.0);
            a11 += boost; a22 += boost;
            det = a11 * a22 - a12 * a12;
        }

        double inv11 =  a22 / det;
        double inv12 = -a12 / det;
        double inv22 =  a11 / det;
        double solX  = inv11 * b1 + inv12 * b2;
        double solY  = inv12 * b1 + inv22 * b2;
        return new double[]{solX, solY};
    }

    private static final class Point {
        final double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }

    private static final class Ray {
        final double px, py;   // origin
        final double vx, vy;   // unit direction
        Ray(double px, double py, double vx, double vy) {
            double n = Math.hypot(vx, vy);
            if (n == 0) throw new IllegalArgumentException("Ray direction cannot be zero");
            this.px = px; this.py = py; this.vx = vx / n; this.vy = vy / n;
        }
    }
}
