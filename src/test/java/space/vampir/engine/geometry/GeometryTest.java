package space.vampir.engine.geometry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeometryTest {

    @Test
    public void testBasicSolve() {
        // Odometry prior: position (10, 10), heading 45°, confidence 0.5
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(10, 10, Math.PI / 4, 0.5);

        // Location detections
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(12, 11, 1.0));

        // YOLO detections
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(20, 20, 30, 2.0));
        yolos.add(new GeometrySolver.YoloDetection(5, 15, -45, 3.0));

        // Solve
        GeometrySolver.Solution solution = GeometrySolver.solve(odometry, locations, yolos);

        // Verify solution exists and converged
        assertNotNull(solution);
        assertTrue(solution.converged, "Solution should converge");
        assertTrue(solution.iterations > 0, "Should take at least one iteration");
        assertTrue(solution.iterations < 100, "Should converge before max iterations");

        System.out.println("=== Basic Solve Test Results ===");
        System.out.printf("Position: (%.3f, %.3f)%n", solution.x, solution.y);
        System.out.printf("Orientation: %.2f degrees%n", solution.alpha * 180 / Math.PI);
        System.out.printf("Iterations: %d%n", solution.iterations);
        System.out.printf("Converged: %b%n", solution.converged);
    }

    @Test
    public void testSolveWithVisualization() throws IOException {
        System.out.println("Starting GeometrySolver visualization test...");

        // Odometry prior: position (10, 10), heading 45°, confidence 0.5
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(10, 10, Math.PI / 4, 0.5);
        System.out.println("Created odometry prior");

        // Location detections
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(12, 11, 1.0));  // radius 1m
        System.out.println("Created location detections");

        // YOLO detections
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(20, 20, 30, 2.0));   // landmark at (20,20), bearing 30°, ±2°
        yolos.add(new GeometrySolver.YoloDetection(5, 15, -45, 3.0));   // landmark at (5,15), bearing -45°, ±3°
        System.out.println("Created YOLO detections");

        // Solve with visualization - save to current folder
        String outputPath = "geometry_solution.png";
        System.out.println("Solving with visualization output to: " + outputPath);
        GeometrySolver.Solution solution = GeometrySolver.solveWithVisualization(odometry, locations, yolos, outputPath);

        // Verify solution
        assertNotNull(solution);
        assertTrue(solution.converged, "Solution should converge");

        System.out.println("=== GeometrySolver Visualization Test Results ===");
        System.out.printf("Position: (%.3f, %.3f)%n", solution.x, solution.y);
        System.out.printf("Orientation: %.2f degrees%n", solution.alpha * 180 / Math.PI);
        System.out.printf("Iterations: %d%n", solution.iterations);
        System.out.printf("Converged: %b%n", solution.converged);
        System.out.printf("Orientation confidence: %.3f (±%.1f°)%n",
                solution.orientationConcentration, solution.orientationUncertainty);
        System.out.println("Visualization saved to: " + outputPath);

        // Verify file was created
        assertTrue(new java.io.File(outputPath).exists(),
                "Visualization file should be created");
    }

    @Test
    public void testOdometryOnly() {
        // Test with only odometry, no other constraints
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(5, 5, 0, 1.0);
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();

        GeometrySolver.Solution solution = GeometrySolver.solve(odometry, locations, yolos);

        // Should return odometry position exactly
        assertEquals(5.0, solution.x, 0.01, "X should match odometry");
        assertEquals(5.0, solution.y, 0.01, "Y should match odometry");
        assertEquals(0.0, solution.alpha, 0.01, "Alpha should match odometry");
    }

    @Test
    public void testLocationDetectionOnly() {
        // Weak odometry, strong location detection
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(0, 0, 0, 0.1);

        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(10, 10, 0.5));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();

        GeometrySolver.Solution solution = GeometrySolver.solve(odometry, locations, yolos);

        // Should be close to location detection
        assertTrue(Math.abs(solution.x - 10.0) < 2.0, "X should be near location detection");
        assertTrue(Math.abs(solution.y - 10.0) < 2.0, "Y should be near location detection");
    }

    @Test
    public void testMultipleYoloDetections() {
        // Test with multiple YOLO landmarks
        GeometrySolver.OdometryPrior odometry = new GeometrySolver.OdometryPrior(10, 10, 0, 0.3);

        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();
        yolos.add(new GeometrySolver.YoloDetection(20, 10, 0, 1.0));    // Landmark directly ahead
        yolos.add(new GeometrySolver.YoloDetection(10, 20, 90, 1.0));   // Landmark to the right
        yolos.add(new GeometrySolver.YoloDetection(0, 10, 180, 1.0));   // Landmark behind

        GeometrySolver.Solution solution = GeometrySolver.solve(odometry, locations, yolos);

        assertNotNull(solution);
        assertTrue(solution.converged, "Should converge with multiple YOLO detections");

        System.out.println("=== Multiple YOLO Test Results ===");
        System.out.printf("Position: (%.3f, %.3f)%n", solution.x, solution.y);
        System.out.printf("Orientation: %.2f degrees%n", solution.alpha * 180 / Math.PI);
    }

    @Test
    public void testConfidenceWeighting() {
        // Test that higher confidence odometry pulls solution closer
        GeometrySolver.OdometryPrior highConfOdo = new GeometrySolver.OdometryPrior(0, 0, 0, 0.9);
        GeometrySolver.OdometryPrior lowConfOdo = new GeometrySolver.OdometryPrior(0, 0, 0, 0.1);

        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(10, 10, 1.0));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();

        GeometrySolver.Solution highConfSolution = GeometrySolver.solve(highConfOdo, locations, yolos);
        GeometrySolver.Solution lowConfSolution = GeometrySolver.solve(lowConfOdo, locations, yolos);

        // High confidence odometry should be closer to origin
        double highConfDist = Math.sqrt(highConfSolution.x * highConfSolution.x + highConfSolution.y * highConfSolution.y);
        double lowConfDist = Math.sqrt(lowConfSolution.x * lowConfSolution.x + lowConfSolution.y * lowConfSolution.y);

        assertTrue(highConfDist < lowConfDist,
                "High confidence odometry should pull solution closer to origin");

        System.out.println("=== Confidence Weighting Test ===");
        System.out.printf("High conf distance from origin: %.3f%n", highConfDist);
        System.out.printf("Low conf distance from origin: %.3f%n", lowConfDist);
    }
}