package space.vampir.engine.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Minimize sum of squared distances from Points and Rays.
 *  Non-negativity on rays enforced via active-set.
 *  Adds a single Odometry point with weight c, and scales all other terms by (1-c).
 *  Also estimates facing orientation from ray bearings + odometry orientation.
 */
public class GeometrySolver {
    private final List<Point> points = new ArrayList<>();
    private final List<Ray> rays = new ArrayList<>();

    // Optional odometry pose prior
    private Point odometry = null;
    private double c = 0.0; // weight on odometry terms in [0,1]

    // Optional odometry orientation prior (unit vector)
    private double odoCos = Double.NaN, odoSin = Double.NaN;

    public void clear() {
        rays.clear();
        points.clear();
        odometry = null;
        c = 0.0;
        odoCos = odoSin = Double.NaN;
    }

    /** Add a regular point (location) constraint (goes into the (1-c) group). */
    public void addPoint(double x, double y) { points.add(new Point(x, y)); }

    /** Add a ray constraint (direction auto-normalized). */
    public void addRay(double px, double py, double vx, double vy) {
        rays.add(new Ray(px, py, vx, vy, 0)); // no bearing info
    }

    /** Add a ray constraint with a car-relative bearing (radians, CCW). */
    public void addRay(double px, double py, double vx, double vy, double betaRad) {
        rays.add(new Ray(px, py, vx, vy, betaRad));
    }

    /** Set (or replace) the odometry point and its weight c in [0,1]. */
    public void setOdometry(double x, double y, double c) {
        if (Double.isNaN(c) || c < 0.0 || c > 1.0)
            throw new IllegalArgumentException("Weight c must be in [0,1]");
        this.odometry = new Point(x, y);
        this.c = c;
    }

    /** Set odometry facing direction (unit). */
    public void setOdometryOrientation(double cosF, double sinF) {
        double n = Math.hypot(cosF, sinF);
        if (n == 0) throw new IllegalArgumentException("Odometry orientation cannot be zero");
        this.odoCos = cosF / n;
        this.odoSin = sinF / n;
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

    /** Estimate facing orientation from bearings + odometry orientation using the same weight c. */
    public OrientationEstimate estimateOrientation(int maxIter) {
        double[] xy = solve(maxIter);
        double x = xy[0], y = xy[1];

        // Build active flags at the solution
        boolean[] active = new boolean[rays.size()];
        for (int i = 0; i < rays.size(); i++) {
            Ray r = rays.get(i);
            double t = r.vx * (x - r.px) + r.vy * (y - r.py);
            active[i] = (t >= 0);
        }

        // Aggregate f_raw = c*f_odo + (1-c)*sum_i w_i * R(-beta_i) * s_i
        double fx = 0.0, fy = 0.0;
        double weightSum = 0.0;

        // detections block (1-c)
        double detectionsWeight = 1.0 - (odometry != null ? c : 0.0); // same policy as for position
        double innerSumX = 0.0, innerSumY = 0.0;
        double innerW = 0.0;

        for (int i = 0; i < rays.size(); i++) {
            Ray r = rays.get(i);
            if (!active[i]) continue;             // use only active rays
            if (Double.isNaN(r.betaRad)) continue; // skip if no bearing info

            // sight vector s_i = (p_i - x*) / ||...||
            double sx = r.px - x;
            double sy = r.py - y;
            double n = Math.hypot(sx, sy);
            if (n < 1e-12) continue;
            sx /= n; sy /= n;

            // rotate by -beta: [cos -sin; sin cos] * s
            double cB = Math.cos(-r.betaRad);
            double sB = Math.sin(-r.betaRad);
            double fix = cB * sx - sB * sy;
            double fiy = sB * sx + cB * sy;

            // weight: simple 1.0 (you may plug quality/t here)
            innerSumX += fix;
            innerSumY += fiy;
            innerW += 1.0;
        }

        if (innerW > 0.0 && detectionsWeight > 0.0) {
            fx += detectionsWeight * innerSumX;
            fy += detectionsWeight * innerSumY;
            weightSum += detectionsWeight * innerW; // for confidence scaling
        }

        // odometry orientation prior (same c block)
        boolean hasOdoDir = !(Double.isNaN(odoCos) || Double.isNaN(odoSin));
        if (hasOdoDir && odometry != null && c > 0.0) {
            fx += c * odoCos;
            fy += c * odoSin;
            weightSum += c;
        }

        double norm = Math.hypot(fx, fy);
        if (norm < 1e-12) {
            return new OrientationEstimate(Double.NaN, Double.NaN, Double.NaN, 0.0);
        }
        double ux = fx / norm, uy = fy / norm;
        double theta = Math.atan2(uy, ux);

        // confidence: mean resultant length normalized to [0,1]
        // denominator ~= ( (1-c)*innerW + c ) when both present
        double denom = 0.0;
        if (innerW > 0.0 && detectionsWeight > 0.0) denom += detectionsWeight * innerW;
        if (hasOdoDir && odometry != null && c > 0.0) denom += c;
        double confidence = (denom > 0.0) ? (norm / denom) : 0.0;

        return new OrientationEstimate(ux, uy, theta, confidence);
    }

    // ---------- READ-ONLY GETTERS (Views) ----------

    public List<PointView> getPoints() {
        List<PointView> out = new ArrayList<>(points.size());
        for (Point p : points) out.add(new PointView(p.x, p.y));
        return out;
    }

    public List<RayView> getRays() {
        List<RayView> out = new ArrayList<>(rays.size());
        for (Ray r : rays) out.add(new RayView(r.px, r.py, r.vx, r.vy, r.betaRad));
        return out;
    }

    public PointView getOdometry() {
        return (odometry == null) ? null : new PointView(odometry.x, odometry.y);
    }

    public double getOdometryWeight() { return c; }

    public OrientationPriorView getOdometryOrientation() {
        if (Double.isNaN(odoCos) || Double.isNaN(odoSin)) return null;
        return new OrientationPriorView(odoCos, odoSin);
    }

    // ---------- Internals ----------

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

    // --------- Internal storage (private), plus public 'views' ---------

    private static final class Point {
        final double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }

    private static final class Ray {
        final double px, py;   // origin (landmark)
        final double vx, vy;   // unit direction (world)
        final double betaRad;  // relative bearing (car frame), CCW; NaN if unknown
        Ray(double px, double py, double vx, double vy, double betaRad) {
            double n = Math.hypot(vx, vy);
            if (n == 0) throw new IllegalArgumentException("Ray direction cannot be zero");
            this.px = px; this.py = py;
            this.vx = vx / n; this.vy = vy / n;
            this.betaRad = betaRad;
        }
    }

    public static final class PointView {
        public final double x, y;
        public PointView(double x, double y) { this.x = x; this.y = y; }
    }

    public static final class RayView {
        public final double px, py, vx, vy, betaRad;
        public RayView(double px, double py, double vx, double vy, double betaRad) {
            this.px = px; this.py = py; this.vx = vx; this.vy = vy; this.betaRad = betaRad;
        }
    }

    /** Immutable orientation estimate. */
    public static final class OrientationEstimate {
        public final double fx, fy;       // unit forward vector
        public final double theta;        // atan2(fy, fx)
        public final double confidence;   // 0..1
        OrientationEstimate(double fx, double fy, double theta, double confidence) {
            this.fx = fx; this.fy = fy; this.theta = theta; this.confidence = confidence;
        }
    }

    /** View of odometry prior orientation. */
    public static final class OrientationPriorView {
        public final double cosF, sinF;
        public OrientationPriorView(double cosF, double sinF) { this.cosF = cosF; this.sinF = sinF; }
    }
}
