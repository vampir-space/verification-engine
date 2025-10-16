package space.vampir.engine.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Single test: one ray from origin at 45°, one location point at (4, 0).
 * Expected solution: (3, 1).
 */
public class GeometryTest {

    @Test
    void solvesRayPlusPointToExpectedLocation() {
        GeometrySolver solver = new GeometrySolver();
        double delta = 1e-6;

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
}
