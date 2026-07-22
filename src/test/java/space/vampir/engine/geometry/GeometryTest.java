package space.vampir.engine.geometry;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class GeometryTest {

    @Test
    public void testWarehouseNavigation() throws IOException {
        // Scenario: Robot navigating in a 50x50m warehouse
        // Odometry has drifted slightly from true position
        // Multiple UWB anchors provide location constraints
        // Several known landmarks (pillars, doors) visible

        System.out.println("\n=== Warehouse Navigation Test ===");

        // Odometry says we're at (25, 30), facing east, with large position uncertainty due to wheel slip
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(25, 30, 10.0, 0);

        // UWB anchor detections (multiple anchors for triangulation)
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(23, 28, 2.0));
        locations.add(new GeometrySolver.LocationDetection(24, 29.5, 1.5));
        locations.add(new GeometrySolver.LocationDetection(23.5, 29, 2.5));

        // Landmark detections: pillars and loading dock door
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(30, 30, Math.toRadians(45), Math.toRadians(2.0)));
        yolos.add(new GeometrySolver.YoloDetection(20, 35, Math.toRadians(135), Math.toRadians(3.0)));
        yolos.add(new GeometrySolver.YoloDetection(25, 25, Math.toRadians(-45), Math.toRadians(2.5)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_warehouse.png");

        assertNotNull(solution);
        System.out.printf("Estimated position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Estimated heading: %.1f°\n", Math.toDegrees(solution.theta));
        System.out.printf("Position uncertainty: σx=%.2f, σy=%.2f\n",
                Math.sqrt(solution.positionCovariance[0][0]),
                Math.sqrt(solution.positionCovariance[1][1]));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Should be close to the UWB cluster
        assertTrue(Math.abs(solution.x - 23.5) < 3.0);
        assertTrue(Math.abs(solution.y - 29.0) < 3.0);
    }

    @Test
    public void testParkingLotLocalization() throws IOException {
        // Scenario: Autonomous vehicle in parking lot
        // GPS provides coarse location, cameras see parking markers

        System.out.println("\n=== Parking Lot Localization Test ===");

        // GPS: (100, 200), heading north, moderate position uncertainty
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                100, 200, 8.0, Math.PI / 2);

        // GPS uncertainty circle (5m radius typical for consumer GPS)
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(101, 198, 5.0));

        // Parking markers and light poles detected by camera
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(105, 205, Math.toRadians(45), Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(95, 205, Math.toRadians(135), Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(100, 210, Math.toRadians(90), Math.toRadians(2.0)));
        yolos.add(new GeometrySolver.YoloDetection(100, 190, Math.toRadians(-90), Math.toRadians(2.0)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_parking.png");

        assertNotNull(solution);
        System.out.printf("Vehicle position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Vehicle heading: %.1f°\n", Math.toDegrees(solution.theta));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Should refine GPS position using visual landmarks
        assertTrue(Math.abs(solution.x - 100) < 3.0);
        assertTrue(Math.abs(solution.y - 200) < 3.0);
    }

    @Test
    public void testCorridorNavigation() throws IOException {
        // Scenario: Robot in narrow corridor with limited side visibility
        // Strong forward/backward constraints, weak lateral constraints

        System.out.println("\n=== Corridor Navigation Test ===");

        // Odometry: moving down corridor at (10, 5), heading east
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(10, 5, 5.0, 0);

        // RFID tag in floor provides lateral constraint only
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(12, 5, 1.0));

        // Can see doors/exits ahead and behind
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(20, 5, 0, Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(0, 5, Math.PI, Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(15, 5.5, Math.toRadians(10), Math.toRadians(2.0)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_corridor.png");

        assertNotNull(solution);
        System.out.printf("Position in corridor: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Should have good longitudinal localization
        assertTrue(Math.abs(solution.x - 12) < 2.0);
        // Y should stay close to corridor center
        assertTrue(Math.abs(solution.y - 5) < 1.5);
    }

    @Test
    public void testOdometryDrift() throws IOException {
        // Scenario: Long-term odometry drift corrected by occasional landmarks
        // Simulates robot that hasn't seen landmarks for a while

        System.out.println("\n=== Odometry Drift Correction Test ===");

        // Odometry thinks we're at (50, 50) but has drifted significantly
        // Very large position uncertainty after long dead-reckoning period
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                50, 50, 15.0, Math.PI / 4);

        // No direct location measurements available
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();

        // Multiple landmarks suddenly come into view - strong correction
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(60, 55, Math.toRadians(30), Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(55, 60, Math.toRadians(60), Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(65, 50, 0, Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(50, 65, Math.toRadians(90), Math.toRadians(1.5)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_drift.png");

        assertNotNull(solution);
        System.out.printf("Corrected position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Drift correction: %.2fm\n",
                Math.sqrt(Math.pow(solution.x - 50, 2) + Math.pow(solution.y - 50, 2)));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Solution should differ significantly from odometry due to landmarks
        double drift = Math.sqrt(Math.pow(solution.x - 50, 2) + Math.pow(solution.y - 50, 2));
        assertTrue(drift > 2.0, "Should correct significant odometry drift");
    }

    @Test
    public void testIntersectionLocalization() throws IOException {
        // Scenario: Vehicle at intersection with traffic signs and lane markers

        System.out.println("\n=== Intersection Localization Test ===");

        // GPS + IMU with small position uncertainty
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(0, 0, 4.0, Math.PI / 6);

        // GPS position
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(1, -0.5, 3.0));

        // Traffic signs and poles at intersection corners
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(10, 10, Math.toRadians(45), Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(-10, 10, Math.toRadians(135), Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(10, -10, Math.toRadians(-45), Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(-10, -10, Math.toRadians(-135), Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(0, 15, Math.toRadians(90), Math.toRadians(2.0)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_intersection.png");

        assertNotNull(solution);
        System.out.printf("Position at intersection: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Heading: %.1f°\n", Math.toDegrees(solution.theta));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Should be near origin with good accuracy from multiple landmarks
        assertTrue(Math.abs(solution.x) < 2.0);
        assertTrue(Math.abs(solution.y) < 2.0);
    }

    @Test
    public void testTightUTurn() throws IOException {
        // Scenario: Robot performing U-turn in confined space
        // High angular velocity may cause odometry errors

        System.out.println("\n=== Tight U-Turn Test ===");

        // Odometry during turn: facing southeast after U-turn, large position uncertainty
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                5, 5, 10.0, -Math.PI * 3 / 4);

        // AprilTag on wall provides strong position constraint
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(6, 4, 0.5));

        // Landmarks help confirm position after turn
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(0, 0, Math.toRadians(135), Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(10, 0, Math.toRadians(45), Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(0, 10, Math.toRadians(-135), Math.toRadians(1.5)));
        yolos.add(new GeometrySolver.YoloDetection(10, 10, Math.toRadians(-45), Math.toRadians(1.5)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_uturn.png");

        assertNotNull(solution);
        System.out.printf("Post-turn position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Post-turn heading: %.1f°\n", Math.toDegrees(solution.theta));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // AprilTag is strong constraint — should pull toward it
        assertTrue(Math.abs(solution.x - 6) < 1.5);
        assertTrue(Math.abs(solution.y - 4) < 1.5);
    }

    @Test
    public void testMultiFloorTransition() throws IOException {
        // Scenario: Robot on elevator or ramp between floors
        // GPS is unreliable, landmarks specific to floor 2

        System.out.println("\n=== Multi-Floor Transition Test ===");

        // GPS confused by floor change — large position uncertainty
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(40, 25, 12.0, 0);

        // GPS position is unreliable
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(38, 22, 8.0));

        // Landmarks specific to floor 2
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(45, 25, 0, Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(35, 25, Math.PI, Math.toRadians(1.0)));
        yolos.add(new GeometrySolver.YoloDetection(40, 30, Math.toRadians(90), Math.toRadians(2.0)));
        yolos.add(new GeometrySolver.YoloDetection(40, 20, Math.toRadians(-90), Math.toRadians(2.0)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_multifloor.png");

        assertNotNull(solution);
        System.out.printf("Position on floor 2: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Landmarks should override unreliable GPS
        assertTrue(Math.abs(solution.x - 40) < 3.0);
        assertTrue(Math.abs(solution.y - 25) < 3.0);
    }

    @Test
    public void testHighSpeedLocalization() throws IOException {
        // Scenario: Vehicle moving at high speed on highway
        // Motion blur and rapid position change

        System.out.println("\n=== High Speed Localization Test ===");

        // High speed on highway, heading northeast, small position uncertainty (good GPS)
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                200, 150, 3.0, Math.PI / 4);

        // GPS still works reasonably well
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(202, 148, 4.0));

        // Distant landmarks with larger angular uncertainty due to speed
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(220, 170, Math.toRadians(45), Math.toRadians(4.0)));
        yolos.add(new GeometrySolver.YoloDetection(180, 130, Math.toRadians(-135), Math.toRadians(4.0)));
        yolos.add(new GeometrySolver.YoloDetection(250, 150, Math.toRadians(15), Math.toRadians(5.0)));

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_highspeed.png");

        assertNotNull(solution);
        System.out.printf("High-speed position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Position uncertainty: %.2fm\n",
                Math.sqrt(solution.positionCovariance[0][0] + solution.positionCovariance[1][1]));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Should maintain localization despite speed
        assertTrue(Math.abs(solution.x - 200) < 5.0);
        assertTrue(Math.abs(solution.y - 150) < 5.0);
    }

    @Test
    public void testMinimalConstraints() {
        // Edge case: Bare minimum constraints

        System.out.println("\n=== Minimal Constraints Test ===");

        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(10, 10, 5.0, 0);

        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(11, 10.5, 2.0));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(15, 10, 0, Math.toRadians(3.0)));

        GeometrySolver.Solution solution = GeometrySolver.solve(odometry, locations, yolos);

        assertNotNull(solution);
        System.out.printf("Minimal constraint solution: (%.2f, %.2f) @ %.1f°\n",
                solution.x, solution.y, Math.toDegrees(solution.theta));
        System.out.printf("Constraints satisfied: %s\n", solution.ok);
    }

    @Test
    public void testOverconstrainedSystem() throws IOException {
        // Scenario: Many overlapping constraints (redundant sensors)
        // Tests solver stability with excess information

        System.out.println("\n=== Overconstrained System Test ===");

        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(50, 50, 5.0, 0);

        // Multiple overlapping UWB measurements
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double x = 51 + 1.5 * Math.cos(angle);
            double y = 49 + 1.5 * Math.sin(angle);
            locations.add(new GeometrySolver.LocationDetection(x, y, 1.0));
        }

        // Many landmarks in all directions
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            double angleDeg = i * 30.0;
            double dist = 10 + i * 2;
            double lx = 51 + dist * Math.cos(Math.toRadians(angleDeg));
            double ly = 49 + dist * Math.sin(Math.toRadians(angleDeg));
            yolos.add(new GeometrySolver.YoloDetection(lx, ly, Math.toRadians(angleDeg), Math.toRadians(1.5)));
        }

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_overconstrained.png");

        assertNotNull(solution);
        System.out.printf("Overconstrained solution: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Used %d location constraints, %d landmarks\n",
                locations.size(), yolos.size());
        System.out.printf("Constraints satisfied: %s\n", solution.ok);

        // Should average all constraints effectively
        assertTrue(Math.abs(solution.x - 51) < 2.0);
        assertTrue(Math.abs(solution.y - 49) < 2.0);

        // Uncertainty should be low with many measurements
        double posUncertainty = Math.sqrt(
                solution.positionCovariance[0][0] + solution.positionCovariance[1][1]);
        System.out.printf("Position uncertainty: %.3fm\n", posUncertainty);
        assertTrue(posUncertainty < 2.0, "Should have low uncertainty with many constraints");
    }
}
