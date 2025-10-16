package space.vampir.engine.geometry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeometryTest {

    @Test
    void oneRayOneOdometry() {
        GeometrySolver solver = new GeometrySolver();
        double delta = 1e-9;

        // Ray from origin at 45°
        double vx = Math.sqrt(0.5);
        double vy = Math.sqrt(0.5);

        // Van egy tábla a (0,0) koordinátában
        // és a 45 fok azt jelenti, hogy a tábla szemszögéből 45 fokba állok én
        // vagyis én az autó, vagyis a táblát én 180+45 fokban látom.
        // Igen Oszkár örülök hogy olvastad.
        // Ne sírj.
        solver.addRay(0.0, 0.0, vx, vy);

        // Odometry point with weight c = 0.5 (others scaled by 1 - c)
        // Beállítom, ezt tudja Oszkár.
        solver.setOdometry(4.0, 0.0, 0.5);

        // Ez amúgy radián nem fok.
        solver.setOdometryOrientation(Math.cos(Math.toRadians(225)), Math.sin(Math.toRadians(225)));

        // És akkor megoldod.
        double[] xy = solver.solve(10);

        // Na és tudjuk hogy ez a megoldás.
        // Expected (same as old test)
        assertEquals(3.0, xy[0], delta, "x should be ~3.0");
        assertEquals(1.0, xy[1], delta, "y should be ~1.0");

        // igen.
        // Ray should be active (t >= 0)
        double t = vx * xy[0] + vy * xy[1];
        assertTrue(t >= -delta, "projection must lie on the ray (t >= 0)");
    }

    @Test
    void twoRaysOneOdometry_integerSolutionWith60_120() {
        GeometrySolver solver = new GeometrySolver();
        double delta = 1e-9;

        // Rays: (0,0) at 60°, (12,0) at 120°
        double vx1 = Math.cos(Math.toRadians(60.0));
        double vy1 = Math.sin(Math.toRadians(60.0));
        solver.addRay(0.0, 0.0, vx1, vy1);

        double vx2 = Math.cos(Math.toRadians(120.0));
        double vy2 = Math.sin(Math.toRadians(120.0));
        solver.addRay(12.0, 0.0, vx2, vy2);

        // Choose odometry with c = 0.5 so the optimum is the integer (6,12)
        solver.setOdometry(6.0, 18.0 - 3.0 * Math.sqrt(3.0), 0.5);
        solver.setOdometryOrientation(Math.cos(Math.toRadians(225.0)), Math.sin(Math.toRadians(225.0)));

        double[] xy = solver.solve(10);

        assertEquals(6.0, xy[0], delta);
        assertEquals(12.0, xy[1], delta);

        // Both rays active
        double t1 = vx1 * (xy[0] - 0.0) + vy1 * (xy[1] - 0.0);
        double t2 = vx2 * (xy[0] - 12.0) + vy2 * (xy[1] - 0.0);
        assertTrue(t1 >= -delta, "ray1 should be active (t1 >= 0)");
        assertTrue(t2 >= -delta, "ray2 should be active (t2 >= 0)");
    }

    @Test
    void visualize_oneRayOneOdometry() {
        GeometrySolver solver = new GeometrySolver();
        double vx = Math.sqrt(0.5), vy = Math.sqrt(0.5);
        solver.addRay(0.0, 0.0, vx, vy);
        solver.setOdometry(4.0, 0.0, 0.1);
        solver.setOdometryOrientation(Math.cos(Math.toRadians(225.0)), Math.sin(Math.toRadians(225.0)));

        GeometryVisualizer.render(solver, "oneRayOneOdo.png",
                800, 600,
                -2, 6,
                -1, 4,
                10);
    }

    @Test
    void visualize_twoRaysOneOdometry() {
        GeometrySolver solver = new GeometrySolver();
        double vx1 = Math.cos(Math.toRadians(60.0));
        double vy1 = Math.sin(Math.toRadians(60.0));
        solver.addRay(0.0, 0.0, vx1, vy1);

        double vx2 = Math.cos(Math.toRadians(120.0));
        double vy2 = Math.sin(Math.toRadians(120.0));
        solver.addRay(12.0, 0.0, vx2, vy2);

        solver.setOdometry(6.0, 18.0 - 3.0 * Math.sqrt(3.0), 0.1);
        solver.setOdometryOrientation(Math.cos(Math.toRadians(225.0)), Math.sin(Math.toRadians(225.0)));

        GeometryVisualizer.render(solver, "twoRaysOneOdo.png",
                900, 700,
                -2, 14,
                -2, 16,
                10);
    }
}
