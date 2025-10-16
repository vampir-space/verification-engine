package space.vampir.engine.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Minimize sum of squared distances from Points and Rays,
 *  non-negativity on rays enforced via active-set.
 */
public class GeometrySolver {
    private final List<Point> points = new ArrayList<>();
    private final List<Ray> rays = new ArrayList<>();

    public void clear() {
        rays.clear();
        points.clear();
    }

    public void addPoint(double x, double y) {
        points.add(new Point(x, y));
    }

    public void addRay(double px, double py, double vx, double vy) {
        rays.add(new Ray(px, py, vx, vy));
    }

    /**
     * Solve for (x,y) using active-set least squares for rays (t>=0) + point constraints.
     * @param maxIter maximum active-set iterations (e.g. 10)
     * @return [x,y] solution
     */
    public double[] solve(int maxIter) {
        double x=0.0, y=0.0;

        // All rays are active (treat as infinite lines)
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
                double t = r.vx*(x - r.px) + r.vy*(y - r.py);
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
        double a11=0, a12=0, a22=0; // symmetric matrix M
        double b1=0, b2=0;          // right-hand side b

        // Rays
        for (int i = 0; i < rays.size(); i++) {
            Ray2D r = rays.get(i);
            if (active[i]) {
                // Line projector A = I - v v^T
                double vv11 = r.vx*r.vx;
                double vv12 = r.vx*r.vy;
                double vv22 = r.vy*r.vy;

                double a11i = 1 - vv11;
                double a12i = - vv12;
                double a22i = 1 - vv22;

                a11 += a11i; a12 += a12i; a22 += a22i;
                b1 += a11i*r.px + a12i*r.py;
                b2 += a12i*r.px + a22i*r.py;

            } else {
                // Blocked ray = point constraint
                a11 += 1; a22 += 1;
                b1 += r.px; b2 += r.py;
            }
        }

        // Points
        for (Point p : points) {
            a11 += 1; a22 += 1;
            b1 += p.x; b2 += p.y;
        }

        // Small regularization to avoid singularity in degenerate cases
        double reg = 1e-12;
        a11 += reg; a22 += reg;

        // Solve 2x2 system M x = b
        double det = a11*a22 - a12*a12;
        if (Math.abs(det) < 1e-18) {
            // Strengthen regularization just in case
            a11 += 1e-8; a22 += 1e-8; det = a11*a22 - a12*a12;
        }

        double inv11 =  a22 / det;
        double inv12 = -a12 / det;
        double inv22 =  a11 / det;
        double solX = inv11*b1 + inv12*b2;
        double solY = inv12*b1 + inv22*b2;
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
            this.px = px; this.py = py; this.vx = vx/n; this.vy = vy/n;
        }
    }
}
