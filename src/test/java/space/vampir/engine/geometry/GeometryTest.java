package space.vampir.engine.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Single test: one ray from origin at 45°, one location point at (4, 0).
 * Expected solution: (3, 1).
 */
public class GeometryTest {

    @Test
    void oneRayOnePoint() {
        GeometrySolver solver = new GeometrySolver();
        double delta = 1e-9;

        // Ray direction at 45 degrees
        double vx = Math.sqrt(0.5);
        double vy = Math.sqrt(0.5);
        solver.addRay(0.0, 0.0, vx, vy);

        // Location (point) constraint
        solver.addPoint(4.0, 0.0);

        double[] xy = solver.solve(10);

        // Expected closed-form for this setup is (3, 1)
        assertEquals(3.0, xy[0], delta, "x should be ~3.0");
        assertEquals(1.0, xy[1], delta, "y should be ~1.0");

        // Additionally, the projection parameter on the ray should be non-negative
        double t = vx * (xy[0] - 0.0) + vy * (xy[1] - 0.0);
        assertTrue(t >= -delta, "projection must lie on the ray (t >= 0)");
    }

    @Test
    void twoRaysOnePoint() {
        GeometrySolver solver = new GeometrySolver();
        double delta = 1e-9;

        // Rays: (0,0) at 60°, (12,0) at 120°
        double vx1 = Math.cos(Math.toRadians(60.0));
        double vy1 = Math.sin(Math.toRadians(60.0));
        solver.addRay(0.0, 0.0, vx1, vy1);

        double vx2 = Math.cos(Math.toRadians(120.0));
        double vy2 = Math.sin(Math.toRadians(120.0));
        solver.addRay(12.0, 0.0, vx2, vy2);

        // Target integer solution (m,n) = (6,12)

        // Point constraint
        double qx = 6;
        double qy = 18.0-3.0*Math.sqrt(3);
        solver.addPoint(qx, qy);

        double[] xy = solver.solve(10);
        assertEquals(6.0,  xy[0], delta);
        assertEquals(12.0, xy[1], delta);

        // Both rays active
        double t1 = vx1*(xy[0])        + vy1*(xy[1]);
        double t2 = vx2*(xy[0] - 12.0) + vy2*(xy[1]);
        assertTrue(t1 >= -delta && t2 >= -delta);
    }

}
