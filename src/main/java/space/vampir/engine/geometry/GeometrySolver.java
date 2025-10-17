import java.util.ArrayList;
import java.util.List;

/**
 * GeometrySolver estimates a vehicle's 2D position and orientation using:
 * 1. Odometry prior
 * 2. Location detections (point constraints)
 * 3. YOLO landmark detections (ray constraints)
 */
public class GeometrySolver {
    
    // Convergence thresholds
    private static final double EPSILON_POS = 0.01;      // meters
    private static final double EPSILON_ANGLE = 0.1;     // degrees
    private static final int MAX_ITERATIONS = 100;
    
    // Regularization constants
    private static final double EPSILON_LOC = 0.01;      // m^2
    private static final double EPSILON_YOLO = 3e-6;     // rad^2
    private static final double LAMBDA_REG = 1e-12;      // numerical stability
    
    // Confidence visualization parameter
    private static final double ORIENTATION_CONE_SCALE = 0.5;
    
    /**
     * Odometry prior: position, orientation, and confidence
     */
    public static class OdometryPrior {
        public double x;
        public double y;
        public double alpha;  // radians
        public double confidence;  // [0, 1]
        
        public OdometryPrior(double x, double y, double alpha, double confidence) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
            this.confidence = Math.max(0, Math.min(1, confidence));
        }
    }
    
    /**
     * Location detection: direct position observation with uncertainty radius
     */
    public static class LocationDetection {
        public double x;
        public double y;
        public double radius;  // uncertainty radius
        
        public LocationDetection(double x, double y, double radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
        
        public double getWeight() {
            return 1.0 / (radius * radius + EPSILON_LOC);
        }
    }
    
    /**
     * YOLO landmark detection: bearing to known landmark
     */
    public static class YoloDetection {
        public double landmarkX;
        public double landmarkY;
        public double bearing;  // degrees, relative to vehicle forward direction
        public double angularUncertainty;  // degrees, half-angle of confidence cone
        
        public YoloDetection(double landmarkX, double landmarkY, double bearing, double angularUncertainty) {
            this.landmarkX = landmarkX;
            this.landmarkY = landmarkY;
            this.bearing = bearing;
            this.angularUncertainty = angularUncertainty;
        }
        
        public double getWeight() {
            double sigmaRad = angularUncertainty * Math.PI / 180.0;
            return 1.0 / (sigmaRad * sigmaRad + EPSILON_YOLO);
        }
    }
    
    /**
     * Solution: estimated pose and confidence measures
     */
    public static class Solution {
        public double x;
        public double y;
        public double alpha;  // radians
        public int iterations;
        public boolean converged;
        
        // Position confidence (covariance matrix)
        public double[][] positionCovariance = new double[2][2];
        
        // Orientation confidence
        public double orientationConcentration;  // [0, 1]
        public double orientationUncertainty;    // degrees, half-angle
        
        public Solution(double x, double y, double alpha) {
            this.x = x;
            this.y = y;
            this.alpha = alpha;
        }
    }
    
    /**
     * Solve for vehicle pose given constraints
     */
    public static Solution solve(
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {
        
        // Initialize with odometry prior
        double x = odometry.x;
        double y = odometry.y;
        double alpha = odometry.alpha;
        
        Solution solution = new Solution(x, y, alpha);
        
        // Iterative optimization
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            double oldX = x;
            double oldY = y;
            double oldAlpha = alpha;
            
            // Step 1: Estimate position (fix orientation)
            double[] position = estimatePosition(x, y, alpha, odometry, locationDetections, yoloDetections);
            x = position[0];
            y = position[1];
            
            // Step 2: Estimate orientation (fix position)
            alpha = estimateOrientation(x, y, alpha, odometry, yoloDetections);
            
            // Check convergence
            double posDelta = Math.sqrt((x - oldX) * (x - oldX) + (y - oldY) * (y - oldY));
            double angleDelta = Math.abs(normalizeAngle(alpha - oldAlpha)) * 180.0 / Math.PI;
            
            solution.iterations = iter + 1;
            
            if (posDelta < EPSILON_POS && angleDelta < EPSILON_ANGLE) {
                solution.converged = true;
                break;
            }
        }
        
        solution.x = x;
        solution.y = y;
        solution.alpha = alpha;
        
        // Compute confidence measures
        computeConfidence(solution, odometry, locationDetections, yoloDetections);
        
        return solution;
    }
    
    /**
     * Estimate position using weighted least squares (orientation fixed)
     */
    private static double[] estimatePosition(
            double x, double y, double alpha,
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {
        
        // Normal equations: M * [x, y]^T = b
        double[][] M = new double[2][2];
        double[] b = new double[2];
        
        double totalWeight = 0;
        double c = odometry.confidence;
        double wOdo = c;
        
        // Compute total non-odometry weight for normalization
        for (LocationDetection loc : locationDetections) {
            totalWeight += loc.getWeight();
        }
        for (YoloDetection yolo : yoloDetections) {
            totalWeight += yolo.getWeight();
        }
        
        // Odometry constraint
        addPointConstraint(M, b, odometry.x, odometry.y, wOdo);
        
        // Location detection constraints
        for (LocationDetection loc : locationDetections) {
            double w = loc.getWeight() * (1 - c);
            addPointConstraint(M, b, loc.x, loc.y, w);
        }
        
        // YOLO ray constraints
        double cosA = Math.cos(alpha);
        double sinA = Math.sin(alpha);
        
        for (YoloDetection yolo : yoloDetections) {
            double w = yolo.getWeight() * (1 - c);
            
            // Rotate forward direction by bearing angle
            double betaRad = yolo.bearing * Math.PI / 180.0;
            double cosBeta = Math.cos(betaRad);
            double sinBeta = Math.sin(betaRad);
            
            // Ray direction: -R(beta) * d
            double rx = -(cosBeta * cosA - sinBeta * sinA);
            double ry = -(sinBeta * cosA + cosBeta * sinA);
            
            // Check if point is in front of or behind landmark
            double dx = x - yolo.landmarkX;
            double dy = y - yolo.landmarkY;
            double t = dx * rx + dy * ry;  // projection along ray
            
            if (t >= 0) {
                // Active ray constraint
                addRayConstraint(M, b, yolo.landmarkX, yolo.landmarkY, rx, ry, w);
            } else {
                // Inactive: treat as point constraint at landmark
                addPointConstraint(M, b, yolo.landmarkX, yolo.landmarkY, w);
            }
        }
        
        // Add regularization
        M[0][0] += LAMBDA_REG;
        M[1][1] += LAMBDA_REG;
        
        // Solve M * p = b
        return solve2x2(M, b);
    }
    
    /**
     * Estimate orientation using weighted circular mean (position fixed)
     */
    private static double estimateOrientation(
            double x, double y, double alpha,
            OdometryPrior odometry,
            List<YoloDetection> yoloDetections) {
        
        double Sx = 0;
        double Sy = 0;
        double totalWeight = 0;
        
        // Odometry orientation
        double wOdo = odometry.confidence;
        Sx += wOdo * Math.cos(odometry.alpha);
        Sy += wOdo * Math.sin(odometry.alpha);
        totalWeight += wOdo;
        
        // YOLO-derived orientations
        for (YoloDetection yolo : yoloDetections) {
            double w = yolo.getWeight() * (1 - odometry.confidence);
            
            // Angle from vehicle to landmark
            double dx = yolo.landmarkX - x;
            double dy = yolo.landmarkY - y;
            double theta = Math.atan2(dy, dx);
            
            // Vehicle heading that would produce this bearing
            double betaRad = yolo.bearing * Math.PI / 180.0;
            double alphaEst = theta - betaRad;
            
            Sx += w * Math.cos(alphaEst);
            Sy += w * Math.sin(alphaEst);
            totalWeight += w;
        }
        
        // Weighted circular mean
        return Math.atan2(Sy, Sx);
    }
    
    /**
     * Add point constraint to normal equations
     */
    private static void addPointConstraint(double[][] M, double[] b, double px, double py, double w) {
        // A = I, so A^T A = I
        M[0][0] += w;
        M[1][1] += w;
        b[0] += w * px;
        b[1] += w * py;
    }
    
    /**
     * Add ray constraint to normal equations
     * Ray starts at (lx, ly) in direction (rx, ry)
     */
    private static void addRayConstraint(double[][] M, double[] b, 
            double lx, double ly, double rx, double ry, double w) {
        
        // Normalize ray direction
        double norm = Math.sqrt(rx * rx + ry * ry);
        double vx = rx / norm;
        double vy = ry / norm;
        
        // Projection operator: A = I - v*v^T
        // A^T A = A (since A is symmetric and idempotent for projection)
        double a00 = 1 - vx * vx;
        double a01 = -vx * vy;
        double a11 = 1 - vy * vy;
        
        M[0][0] += w * a00;
        M[0][1] += w * a01;
        M[1][0] += w * a01;
        M[1][1] += w * a11;
        
        // b = A^T A p, where p is the landmark position
        b[0] += w * (a00 * lx + a01 * ly);
        b[1] += w * (a01 * lx + a11 * ly);
    }
    
    /**
     * Solve 2x2 linear system
     */
    private static double[] solve2x2(double[][] M, double[] b) {
        double det = M[0][0] * M[1][1] - M[0][1] * M[1][0];
        
        if (Math.abs(det) < 1e-15) {
            // Singular matrix, return zero
            return new double[]{0, 0};
        }
        
        double invDet = 1.0 / det;
        double x = invDet * (M[1][1] * b[0] - M[0][1] * b[1]);
        double y = invDet * (-M[1][0] * b[0] + M[0][0] * b[1]);
        
        return new double[]{x, y};
    }
    
    /**
     * Compute confidence measures
     */
    private static void computeConfidence(
            Solution solution,
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {
        
        // Recompute M matrix for position covariance
        double[][] M = new double[2][2];
        double[] dummy = new double[2];
        
        double c = odometry.confidence;
        double wOdo = c;
        
        addPointConstraint(M, dummy, odometry.x, odometry.y, wOdo);
        
        for (LocationDetection loc : locationDetections) {
            double w = loc.getWeight() * (1 - c);
            addPointConstraint(M, dummy, loc.x, loc.y, w);
        }
        
        double cosA = Math.cos(solution.alpha);
        double sinA = Math.sin(solution.alpha);
        
        for (YoloDetection yolo : yoloDetections) {
            double w = yolo.getWeight() * (1 - c);
            
            double betaRad = yolo.bearing * Math.PI / 180.0;
            double cosBeta = Math.cos(betaRad);
            double sinBeta = Math.sin(betaRad);
            
            double rx = -(cosBeta * cosA - sinBeta * sinA);
            double ry = -(sinBeta * cosA + cosBeta * sinA);
            
            double dx = solution.x - yolo.landmarkX;
            double dy = solution.y - yolo.landmarkY;
            double t = dx * rx + dy * ry;
            
            if (t >= 0) {
                addRayConstraint(M, dummy, yolo.landmarkX, yolo.landmarkY, rx, ry, w);
            } else {
                addPointConstraint(M, dummy, yolo.landmarkX, yolo.landmarkY, w);
            }
        }
        
        M[0][0] += LAMBDA_REG;
        M[1][1] += LAMBDA_REG;
        
        // Position covariance = M^{-1}
        double det = M[0][0] * M[1][1] - M[0][1] * M[1][0];
        if (Math.abs(det) > 1e-15) {
            double invDet = 1.0 / det;
            solution.positionCovariance[0][0] = invDet * M[1][1];
            solution.positionCovariance[0][1] = -invDet * M[0][1];
            solution.positionCovariance[1][0] = -invDet * M[1][0];
            solution.positionCovariance[1][1] = invDet * M[0][0];
        }
        
        // Orientation confidence
        double Sx = 0, Sy = 0, totalWeight = 0;
        
        Sx += wOdo * Math.cos(odometry.alpha);
        Sy += wOdo * Math.sin(odometry.alpha);
        totalWeight += wOdo;
        
        for (YoloDetection yolo : yoloDetections) {
            double w = yolo.getWeight() * (1 - c);
            totalWeight += w;
        }
        
        // Concentration measure
        double r = Math.sqrt(Sx * Sx + Sy * Sy) / totalWeight;
        solution.orientationConcentration = r;
        solution.orientationUncertainty = ORIENTATION_CONE_SCALE * (1 - r) * 180.0;
    }
    
    /**
     * Normalize angle to [-pi, pi]
     */
    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    /**
     * Example usage
     */
    public static void main(String[] args) {
        // Odometry prior: position (10, 10), heading 45°, confidence 0.5
        OdometryPrior odometry = new OdometryPrior(10, 10, Math.PI / 4, 0.5);
        
        // Location detections
        List<LocationDetection> locations = new ArrayList<>();
        locations.add(new LocationDetection(12, 11, 1.0));  // radius 1m
        
        // YOLO detections
        List<YoloDetection> yolos = new ArrayList<>();
        yolos.add(new YoloDetection(20, 20, 30, 2.0));   // landmark at (20,20), bearing 30°, ±2°
        yolos.add(new YoloDetection(5, 15, -45, 3.0));   // landmark at (5,15), bearing -45°, ±3°
        
        // Solve
        Solution solution = solve(odometry, locations, yolos);
        
        System.out.println("=== GeometrySolver Results ===");
        System.out.printf("Position: (%.3f, %.3f)%n", solution.x, solution.y);
        System.out.printf("Orientation: %.2f degrees%n", solution.alpha * 180 / Math.PI);
        System.out.printf("Iterations: %d%n", solution.iterations);
        System.out.printf("Converged: %b%n", solution.converged);
        System.out.printf("Orientation confidence: %.3f (±%.1f°)%n", 
                solution.orientationConcentration, solution.orientationUncertainty);
    }
}