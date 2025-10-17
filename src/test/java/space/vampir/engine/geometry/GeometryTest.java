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

        // Odometry says we're at (25, 30), facing east, but with low confidence due to wheel slip
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(25, 30, 0, 0.3);

        // UWB anchor detections (multiple anchors for triangulation)
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(23, 28, 2.0));  // UWB anchor 1
        locations.add(new GeometrySolver.LocationDetection(24, 29.5, 1.5)); // UWB anchor 2
        locations.add(new GeometrySolver.LocationDetection(23.5, 29, 2.5)); // UWB anchor 3

        // Landmark detections: pillars and loading dock door
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(30, 30, 45, 2.0));    // Pillar at 45° right
        yolos.add(new GeometrySolver.YoloDetection(20, 35, 135, 3.0));   // Loading door
        yolos.add(new GeometrySolver.YoloDetection(25, 25, -45, 2.5));   // Another pillar

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_warehouse.png");

        assertTrue(solution.converged);
        System.out.printf("Estimated position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Estimated heading: %.1f°\n", Math.toDegrees(solution.alpha));
        System.out.printf("Position uncertainty: σx=%.2f, σy=%.2f\n",
                Math.sqrt(solution.positionCovariance[0][0]),
                Math.sqrt(solution.positionCovariance[1][1]));

        // Should be close to the UWB cluster
        assertTrue(Math.abs(solution.x - 23.5) < 3.0);
        assertTrue(Math.abs(solution.y - 29.0) < 3.0);
    }

    @Test
    public void testParkingLotLocalization() throws IOException {
        // Scenario: Autonomous vehicle in parking lot
        // GPS provides coarse location, cameras see parking markers

        System.out.println("\n=== Parking Lot Localization Test ===");

        // GPS: (100, 200), heading north, moderate confidence
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                100, 200, Math.PI/2, 0.4);

        // GPS uncertainty circle (5m radius typical for consumer GPS)
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(101, 198, 5.0));

        // Parking markers and light poles detected by camera
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(105, 205, 45, 1.5));   // Parking marker
        yolos.add(new GeometrySolver.YoloDetection(95, 205, 135, 1.5));   // Parking marker
        yolos.add(new GeometrySolver.YoloDetection(100, 210, 90, 2.0));   // Light pole ahead
        yolos.add(new GeometrySolver.YoloDetection(100, 190, -90, 2.0));  // Light pole behind

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_parking.png");

        assertTrue(solution.converged);
        System.out.printf("Vehicle position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Vehicle heading: %.1f°\n", Math.toDegrees(solution.alpha));

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
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                10, 5, 0, 0.5);

        // RFID tag in floor provides lateral constraint only
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(12, 5, 1.0));

        // Can see doors/exits ahead and behind
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(20, 5, 0, 1.0));     // Exit ahead
        yolos.add(new GeometrySolver.YoloDetection(0, 5, 180, 1.5));    // Entrance behind
        yolos.add(new GeometrySolver.YoloDetection(15, 5.5, 10, 2.0));  // Fire extinguisher

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_corridor.png");

        assertTrue(solution.converged);
        System.out.printf("Position in corridor: (%.2f, %.2f)\n", solution.x, solution.y);

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
        // Very low confidence after long dead-reckoning period
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                50, 50, Math.PI/4, 0.15);

        // No direct location measurements available
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();

        // Multiple landmarks suddenly come into view - strong correction
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(60, 55, 30, 1.0));   // Wall corner
        yolos.add(new GeometrySolver.YoloDetection(55, 60, 60, 1.0));   // Column
        yolos.add(new GeometrySolver.YoloDetection(65, 50, 0, 1.5));    // Doorway
        yolos.add(new GeometrySolver.YoloDetection(50, 65, 90, 1.5));   // Alcove

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_drift.png");

        assertTrue(solution.converged);
        System.out.printf("Corrected position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Drift correction: %.2fm\n",
                Math.sqrt(Math.pow(solution.x - 50, 2) + Math.pow(solution.y - 50, 2)));

        // Solution should differ significantly from odometry due to landmarks
        double drift = Math.sqrt(Math.pow(solution.x - 50, 2) + Math.pow(solution.y - 50, 2));
        assertTrue(drift > 2.0, "Should correct significant odometry drift");
    }

    @Test
    public void testIntersectionLocalization() throws IOException {
        // Scenario: Vehicle at intersection with traffic signs and lane markers

        System.out.println("\n=== Intersection Localization Test ===");

        // GPS + IMU
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                0, 0, Math.PI/6, 0.6);

        // GPS position
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(1, -0.5, 3.0));

        // Traffic signs and poles at intersection corners
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(10, 10, 45, 1.0));    // NE corner sign
        yolos.add(new GeometrySolver.YoloDetection(-10, 10, 135, 1.0));  // NW corner sign
        yolos.add(new GeometrySolver.YoloDetection(10, -10, -45, 1.0));  // SE corner sign
        yolos.add(new GeometrySolver.YoloDetection(-10, -10, -135, 1.0)); // SW corner sign
        yolos.add(new GeometrySolver.YoloDetection(0, 15, 90, 2.0));     // Traffic light

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_intersection.png");

        assertTrue(solution.converged);
        System.out.printf("Position at intersection: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Heading: %.1f°\n", Math.toDegrees(solution.alpha));

        // Should be near origin with good accuracy from multiple landmarks
        assertTrue(Math.abs(solution.x) < 2.0);
        assertTrue(Math.abs(solution.y) < 2.0);
    }

    @Test
    public void testTightUTurn() throws IOException {
        // Scenario: Robot performing U-turn in confined space
        // High angular velocity may cause odometry errors

        System.out.println("\n=== Tight U-Turn Test ===");

        // Odometry during turn: facing southeast after attempting U-turn
        // Low confidence due to rapid rotation
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                5, 5, -Math.PI * 3/4, 0.25);

        // AprilTag on wall provides strong position constraint
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(6, 4, 0.5));

        // Landmarks help determine actual heading after turn
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(0, 0, 135, 1.5));    // Corner behind
        yolos.add(new GeometrySolver.YoloDetection(10, 0, 45, 1.5));    // Corner behind
        yolos.add(new GeometrySolver.YoloDetection(0, 10, -135, 1.5));  // Wall ahead-left
        yolos.add(new GeometrySolver.YoloDetection(10, 10, -45, 1.5));  // Wall ahead-right

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_uturn.png");

        assertTrue(solution.converged);
        System.out.printf("Post-turn position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Post-turn heading: %.1f°\n", Math.toDegrees(solution.alpha));

        // Should correct heading error from rapid rotation
        assertTrue(Math.abs(solution.x - 6) < 1.5);
        assertTrue(Math.abs(solution.y - 4) < 1.5);
    }

    @Test
    public void testMultiFloorTransition() throws IOException {
        // Scenario: Robot on elevator or ramp between floors
        // Z-coordinate change causes GPS anomaly, landmarks help

        System.out.println("\n=== Multi-Floor Transition Test ===");

        // GPS confused by floor change
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                40, 25, 0, 0.2);

        // GPS position is unreliable
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(38, 22, 8.0));  // Large uncertainty

        // Landmarks specific to floor 2
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(45, 25, 0, 1.0));     // Room 201 sign
        yolos.add(new GeometrySolver.YoloDetection(35, 25, 180, 1.0));   // Elevator door
        yolos.add(new GeometrySolver.YoloDetection(40, 30, 90, 2.0));    // Fire exit
        yolos.add(new GeometrySolver.YoloDetection(40, 20, -90, 2.0));   // Stairwell

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_multifloor.png");

        assertTrue(solution.converged);
        System.out.printf("Position on floor 2: (%.2f, %.2f)\n", solution.x, solution.y);

        // Landmarks should override unreliable GPS
        assertTrue(Math.abs(solution.x - 40) < 3.0);
        assertTrue(Math.abs(solution.y - 25) < 3.0);
    }

    @Test
    public void testHighSpeedLocalization() throws IOException {
        // Scenario: Vehicle moving at high speed on highway
        // Motion blur and rapid position change

        System.out.println("\n=== High Speed Localization Test ===");

        // High speed on highway, heading northeast
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                200, 150, Math.PI/4, 0.7);

        // GPS still works reasonably well
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(202, 148, 4.0));

        // Distant landmarks with larger angular uncertainty due to speed
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(220, 170, 45, 4.0));   // Exit sign
        yolos.add(new GeometrySolver.YoloDetection(180, 130, -135, 4.0)); // Previous exit
        yolos.add(new GeometrySolver.YoloDetection(250, 150, 15, 5.0));   // Water tower

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_highspeed.png");

        assertTrue(solution.converged);
        System.out.printf("High-speed position: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Position uncertainty: %.2fm\n",
                Math.sqrt(solution.positionCovariance[0][0] + solution.positionCovariance[1][1]));

        // Should maintain localization despite speed
        assertTrue(Math.abs(solution.x - 200) < 5.0);
        assertTrue(Math.abs(solution.y - 150) < 5.0);
    }

    @Test
    public void testMinimalConstraints() {
        // Edge case: Bare minimum constraints - should still converge

        System.out.println("\n=== Minimal Constraints Test ===");

        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                10, 10, 0, 0.5);

        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(11, 10.5, 2.0));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(15, 10, 0, 3.0));

        GeometrySolver.Solution solution = GeometrySolver.solve(odometry, locations, yolos);

        assertTrue(solution.converged);
        System.out.printf("Minimal constraint solution: (%.2f, %.2f) @ %.1f°\n",
                solution.x, solution.y, Math.toDegrees(solution.alpha));
    }

    @Test
    public void testOverconstrainedSystem() throws IOException {
        // Scenario: Many overlapping constraints (redundant sensors)
        // Tests solver stability with excess information

        System.out.println("\n=== Overconstrained System Test ===");

        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(
                50, 50, 0, 0.5);

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
            double angle = i * 30;  // Every 30 degrees
            double dist = 10 + i * 2;
            double lx = 51 + dist * Math.cos(Math.toRadians(angle));
            double ly = 49 + dist * Math.sin(Math.toRadians(angle));
            yolos.add(new GeometrySolver.YoloDetection(lx, ly, angle, 1.5));
        }

        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(
                odometry, locations, yolos, "geometryTests/test_overconstrained.png");

        assertTrue(solution.converged);
        System.out.printf("Overconstrained solution: (%.2f, %.2f)\n", solution.x, solution.y);
        System.out.printf("Used %d location constraints, %d landmarks\n",
                locations.size(), yolos.size());

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