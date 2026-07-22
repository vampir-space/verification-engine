package space.vampir.engine.geometry;

import java.util.List;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * GeometrySolver estimates a vehicle's 2D position and orientation using:
 * 1. Odometry prior
 * 2. Location detections (point constraints)
 * 3. YOLO landmark detections (ray constraints)
 */
public class GeometrySolver {

    // Regularization constant for numerical stability
    private static final double LAMBDA_REG = 1e-12;

    /**
     * Odometry prior: position, orientation, and confidence
     */
    public static class OdometryPrior {
        public double x;
        public double y;
        public double xyRadius;
        public double theta;  // radians, known with certainty

        public OdometryPrior(double x, double y, double xyRadius, double theta) {
            this.x = x;
            this.y = y;
            this.xyRadius = xyRadius;
            this.theta = theta;
        }

        public double getWeight() { return 0.5; }
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

        public double getWeight() { return 1.0; }
    }

    /**
     * YOLO landmark detection: bearing to known landmark
     */
    public static class YoloDetection {
        public double landmarkX;
        public double landmarkY;
        public double bearing;  // radians, relative to vehicle forward direction
        public double bearingHalfCone;  // radians, half-angle of confidence cone

        public YoloDetection(double landmarkX, double landmarkY, double bearing, double bearingHalfCone) {
            this.landmarkX = landmarkX;
            this.landmarkY = landmarkY;
            this.bearing = bearing;
            this.bearingHalfCone = bearingHalfCone;
        }

        public double getWeight() { return 1.0; }
    }

    /**
     * Solution: estimated pose and confidence measures
     */
    public static class Solution {
        public double x;
        public double y;
        public double theta;  // radians
        public boolean ok;

        // Position confidence (covariance matrix)
        public double[][] positionCovariance = new double[2][2];

        // Orientation confidence
        public double orientationConcentration;  // [0, 1]
        public double orientationUncertainty;    // radians, half-angle

        public Solution(double x, double y, double theta) {
            this.x = x;
            this.y = y;
            this.theta = theta;
        }
    }

    /**
     * Solve for vehicle position given constraints.
     * Orientation theta is taken as certain from odometry.
     * Finds (x, y) minimizing weighted squared error across all constraints.
     * Sets ok=true if every constraint is individually satisfied by the solution.
     */
    public static Solution solve(
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {

        double theta = odometry.theta;
        double[] position = estimatePosition(theta, odometry, locationDetections, yoloDetections);

        Solution solution = new Solution(position[0], position[1], theta);
        solution.ok = checkConstraints(solution, odometry, locationDetections, yoloDetections);

        computeConfidence(solution, odometry, locationDetections, yoloDetections);

        return solution;
    }

    /**
     * Returns true if every constraint is satisfied by the solution:
     *   - OdometryPrior: solution within xyRadius
     *   - LocationDetection: solution within radius
     *   - YoloDetection: bearing to landmark within bearingHalfCone
     */
    private static boolean checkConstraints(
            Solution solution,
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {

        if (Math.hypot(solution.x - odometry.x, solution.y - odometry.y) > odometry.xyRadius) return false;

        for (LocationDetection loc : locationDetections) {
            if (Math.hypot(solution.x - loc.x, solution.y - loc.y) > loc.radius) return false;
        }

        for (YoloDetection yolo : yoloDetections) {
            double dx = yolo.landmarkX - solution.x;
            double dy = yolo.landmarkY - solution.y;
            double actualBearing = normalizeAngle(Math.atan2(dy, dx) - solution.theta);
            if (Math.abs(normalizeAngle(actualBearing - yolo.bearing)) > yolo.bearingHalfCone) return false;
        }

        return true;
    }

    /**
     * Weighted least squares position estimate with theta fixed.
     * Active-set check uses the odometry position as reference.
     */
    private static double[] estimatePosition(
            double theta,
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {

        double[][] M = new double[2][2];
        double[] b = new double[2];

        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);

        addPointConstraint(M, b, odometry.x, odometry.y, odometry.getWeight());

        for (LocationDetection loc : locationDetections) {
            addPointConstraint(M, b, loc.x, loc.y, loc.getWeight());
        }

        for (YoloDetection yolo : yoloDetections) {
            // Check if odometry reference falls inside the bearing cone
            double refBearing = normalizeAngle(Math.atan2(yolo.landmarkY - odometry.y, yolo.landmarkX - odometry.x) - theta);
            double deviation = normalizeAngle(refBearing - yolo.bearing);

            if (Math.abs(deviation) <= yolo.bearingHalfCone) {
                // Inside cone: constraint satisfied, skip
                continue;
            }

            // Outside cone: snap to nearest cone boundary
            double activeBearing = yolo.bearing + Math.signum(deviation) * yolo.bearingHalfCone;

            double cosBeta = Math.cos(activeBearing);
            double sinBeta = Math.sin(activeBearing);

            // Ray direction from landmark toward vehicle: -R(bearing) * d
            double rx = -(cosBeta * cosT - sinBeta * sinT);
            double ry = -(sinBeta * cosT + cosBeta * sinT);

            // Active-set: use odometry position to determine which side of the landmark we're on
            double dx = odometry.x - yolo.landmarkX;
            double dy = odometry.y - yolo.landmarkY;
            double t = dx * rx + dy * ry;

            if (t >= 0) {
                addRayConstraint(M, b, yolo.landmarkX, yolo.landmarkY, rx, ry, yolo.getWeight());
            } else {
                addPointConstraint(M, b, yolo.landmarkX, yolo.landmarkY, yolo.getWeight());
            }
        }

        M[0][0] += LAMBDA_REG;
        M[1][1] += LAMBDA_REG;

        return solve2x2(M, b);
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

    private static void computeConfidence(
            Solution solution,
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections) {

        // Rebuild M from the same constraints used in estimatePosition to get position covariance
        double[][] M = new double[2][2];
        double[] dummy = new double[2];

        double cosT = Math.cos(solution.theta);
        double sinT = Math.sin(solution.theta);

        addPointConstraint(M, dummy, odometry.x, odometry.y, odometry.getWeight());

        for (LocationDetection loc : locationDetections) {
            addPointConstraint(M, dummy, loc.x, loc.y, loc.getWeight());
        }

        for (YoloDetection yolo : yoloDetections) {
            // Mirror the same cone check used in estimatePosition (odometry as reference)
            double refBearing = normalizeAngle(Math.atan2(yolo.landmarkY - odometry.y, yolo.landmarkX - odometry.x) - solution.theta);
            double deviation = normalizeAngle(refBearing - yolo.bearing);

            if (Math.abs(deviation) <= yolo.bearingHalfCone) {
                continue;
            }

            double activeBearing = yolo.bearing + Math.signum(deviation) * yolo.bearingHalfCone;

            double cosBeta = Math.cos(activeBearing);
            double sinBeta = Math.sin(activeBearing);

            double rx = -(cosBeta * cosT - sinBeta * sinT);
            double ry = -(sinBeta * cosT + cosBeta * sinT);

            double dx = odometry.x - yolo.landmarkX;
            double dy = odometry.y - yolo.landmarkY;
            double t = dx * rx + dy * ry;

            if (t >= 0) {
                addRayConstraint(M, dummy, yolo.landmarkX, yolo.landmarkY, rx, ry, yolo.getWeight());
            } else {
                addPointConstraint(M, dummy, yolo.landmarkX, yolo.landmarkY, yolo.getWeight());
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

        // Theta is known with certainty
        solution.orientationConcentration = 1.0;
        solution.orientationUncertainty = 0.0;
    }

    /**
     * Solve with visualization output
     */
    public static Solution solveWithVisualization(
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections,
            String outputFilePath) throws IOException {

        // First solve normally
        Solution solution = solve(odometry, locationDetections, yoloDetections);

        // Generate visualization
        generateVisualization(solution, odometry, locationDetections, yoloDetections, outputFilePath);

        return solution;
    }

    /** Strongly-typed coordinate transform (replaces reflection hacks) */
    private static final class CoordTransform {
        final double minX, minY, scale;
        final int margin, height;
        CoordTransform(double minX, double minY, double scale, int margin, int height) {
            this.minX = minX; this.minY = minY; this.scale = scale; this.margin = margin; this.height = height;
        }
        int toScreenX(double x) { return margin + (int)((x - minX) * scale); }
        int toScreenY(double y) { return height - margin - (int)((y - minY) * scale); }
        double toScreenScale(double d) { return d * scale; }
    }

    /**
     * Generate PNG visualization of the solver results
     */
    private static void generateVisualization(
            Solution solution,
            OdometryPrior odometry,
            List<LocationDetection> locationDetections,
            List<YoloDetection> yoloDetections,
            String outputFilePath) throws IOException {

        // Set headless mode for environments without display
        System.setProperty("java.awt.headless", "true");

        // Image dimensions
        int width = 1200;
        int height = 1200;
        int margin = 100;

        // Calculate bounds for all points
        double minX = odometry.x;
        double maxX = odometry.x;
        double minY = odometry.y;
        double maxY = odometry.y;

        for (LocationDetection loc : locationDetections) {
            minX = Math.min(minX, loc.x - loc.radius);
            maxX = Math.max(maxX, loc.x + loc.radius);
            minY = Math.min(minY, loc.y - loc.radius);
            maxY = Math.max(maxY, loc.y + loc.radius);
        }

        for (YoloDetection yolo : yoloDetections) {
            minX = Math.min(minX, yolo.landmarkX);
            maxX = Math.max(maxX, yolo.landmarkX);
            minY = Math.min(minY, yolo.landmarkY);
            maxY = Math.max(maxY, yolo.landmarkY);
        }

        minX = Math.min(minX, solution.x);
        maxX = Math.max(maxX, solution.x);
        minY = Math.min(minY, solution.y);
        maxY = Math.max(maxY, solution.y);

        // Add padding
        double padding = Math.max(maxX - minX, maxY - minY) * 0.2;
        minX -= padding;
        maxX += padding;
        minY -= padding;
        maxY += padding;

        // Scale factor
        double scaleX = (width - 2 * margin) / (maxX - minX);
        double scaleY = (height - 2 * margin) / (maxY - minY);
        double scale = Math.min(scaleX, scaleY);

        // Create image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Coordinate transform
        CoordTransform ct = new CoordTransform(minX, minY, scale, margin, height);

        // Draw grid
        g.setColor(new Color(230, 230, 230));
        g.setStroke(new BasicStroke(1));
        for (int i = 0; i < 10; i++) {
            double x = minX + (maxX - minX) * i / 10.0;
            double y = minY + (maxY - minY) * i / 10.0;
            g.drawLine(ct.toScreenX(x), ct.toScreenY(minY), ct.toScreenX(x), ct.toScreenY(maxY));
            g.drawLine(ct.toScreenX(minX), ct.toScreenY(y), ct.toScreenX(maxX), ct.toScreenY(y));
        }

        // Draw location detections (blue dots and circles)
        for (LocationDetection loc : locationDetections) {
            int sx = ct.toScreenX(loc.x);
            int sy = ct.toScreenY(loc.y);
            int radius = (int)ct.toScreenScale(loc.radius);

            // Transparent circle
            g.setColor(new Color(0, 0, 255, 50));
            g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);

            // Circle outline
            g.setColor(new Color(0, 0, 255, 150));
            g.setStroke(new BasicStroke(2));
            g.drawOval(sx - radius, sy - radius, radius * 2, radius * 2);

            // Center dot
            g.setColor(Color.BLUE);
            g.fillOval(sx - 5, sy - 5, 10, 10);
        }

        // Draw YOLO detections (green dots with numbers and cones)
        g.setFont(new Font("Arial", Font.BOLD, 14));
        for (int i = 0; i < yoloDetections.size(); i++) {
            YoloDetection yolo = yoloDetections.get(i);
            int sx = ct.toScreenX(yolo.landmarkX);
            int sy = ct.toScreenY(yolo.landmarkY);

            // Green dot
            g.setColor(new Color(0, 180, 0));
            g.fillOval(sx - 6, sy - 6, 12, 12);

            // Number label
            g.setColor(Color.BLACK);
            String label = String.valueOf(i + 1);
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g.drawString(label, sx - labelWidth / 2, sy - 12);

            // Draw cone from solution to landmark
            double dx = yolo.landmarkX - solution.x;
            double dy = yolo.landmarkY - solution.y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > 0.01) {
                // Expected angle based on bearing measurement
                double bearingRad = yolo.bearing;
                double expectedAngle = solution.theta + bearingRad;

                // Cone half-angle
                double coneHalfAngle = yolo.bearingHalfCone;

                // Draw cone
                int solX = ct.toScreenX(solution.x);
                int solY = ct.toScreenY(solution.y);

                double coneLength = ct.toScreenScale(dist);

                // Create cone polygon
                Path2D.Double conePath = new Path2D.Double();
                conePath.moveTo(solX, solY);

                double angle1 = expectedAngle + coneHalfAngle;
                double angle2 = expectedAngle - coneHalfAngle;

                // Draw arc
                int numSegments = 20;
                for (int j = 0; j <= numSegments; j++) {
                    double angle = angle1 + (angle2 - angle1) * j / numSegments;
                    double cx = solX + coneLength * Math.cos(angle);
                    double cy = solY - coneLength * Math.sin(angle);
                    conePath.lineTo(cx, cy);
                }
                conePath.closePath();

                // Fill cone (transparent green)
                g.setColor(new Color(0, 255, 0, 40));
                g.fill(conePath);

                // Draw cone outline
                g.setColor(new Color(0, 180, 0, 120));
                g.setStroke(new BasicStroke(2));
                g.draw(conePath);

                // Draw center ray
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10, new float[]{5, 5}, 0));
                double centerX = solX + coneLength * Math.cos(expectedAngle);
                double centerY = solY - coneLength * Math.sin(expectedAngle);
                g.drawLine(solX, solY, (int)centerX, (int)centerY);

                // Draw cone number at the edge
                g.setColor(new Color(0, 120, 0));
                g.setFont(new Font("Arial", Font.BOLD, 12));
                int labelX = (int)(solX + coneLength * 0.95 * Math.cos(expectedAngle));
                int labelY = (int)(solY - coneLength * 0.95 * Math.sin(expectedAngle));
                g.drawString(String.valueOf(i + 1), labelX, labelY);
            }
        }

        // Draw odometry prior (red dot, circle, and arrow)
        int odoX = ct.toScreenX(odometry.x);
        int odoY = ct.toScreenY(odometry.y);

        double odoRadius = odometry.xyRadius;
        int odoRadiusPx = (int)ct.toScreenScale(odoRadius);
        g.setColor(new Color(255, 0, 0, 50));
        g.fillOval(odoX - odoRadiusPx, odoY - odoRadiusPx, odoRadiusPx * 2, odoRadiusPx * 2);

        g.setColor(new Color(255, 0, 0, 150));
        g.setStroke(new BasicStroke(2));
        g.drawOval(odoX - odoRadiusPx, odoY - odoRadiusPx, odoRadiusPx * 2, odoRadiusPx * 2);

        // Red dot
        g.setColor(Color.RED);
        g.fillOval(odoX - 6, odoY - 6, 12, 12);

        // Odometry orientation arrow
        drawArrow(g, odoX, odoY, odometry.theta, (int)ct.toScreenScale(odoRadius * 0.8), Color.RED);

        // Draw solution (black dot and arrow)
        int solX = ct.toScreenX(solution.x);
        int solY = ct.toScreenY(solution.y);

        g.setColor(Color.BLACK);
        g.fillOval(solX - 7, solY - 7, 14, 14);

        // Solution orientation arrow
        drawArrow(g, solX, solY, solution.theta, (int)ct.toScreenScale(odoRadius * 0.8), Color.BLACK);

        // === NEW: Location confidence ellipse (black, semi-transparent, ~95%) ===
        drawLocationConfidenceEllipse(g, ct, solution);

        // === NEW: Orientation confidence cone (black, semi-transparent) ===
        drawOrientationConfidenceCone(g, ct, solution, minX, maxX, minY, maxY);

        // ===== UPDATED LEGEND =====
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        int legendX = 20;
        int legendY = 30;
        int lineHeight = 22;
        int row = 0;

        g.setColor(Color.BLACK);
        g.drawString("Legend:", legendX, legendY + row * lineHeight); row++;

        // 1) Solution: location & orientation
        g.setColor(Color.BLACK);
        g.fillOval(legendX, legendY + row * lineHeight - 6, 12, 12);
        g.setColor(Color.BLACK);
        g.drawString(String.format("Solution: (%.2f, %.2f), orientation %.1f°",
                        solution.x, solution.y, solution.theta * 180.0 / Math.PI),
                legendX + 22, legendY + row * lineHeight);
        row++;

        // 2) Confidence: location ellipse
        // small ellipse icon
        g.setColor(new Color(0, 0, 0, 50));
        g.fillOval(legendX, legendY + row * lineHeight - 8, 16, 16);
        g.setColor(new Color(0, 0, 0, 140));
        g.drawOval(legendX, legendY + row * lineHeight - 8, 16, 16);
        g.setColor(Color.BLACK);
        g.drawString("Confidence (location): covariance ellipse (~95%)", legendX + 22, legendY + row * lineHeight);
        row++;

        // 2) Confidence: orientation cone (mini wedge)
        drawMiniConeKey(g, legendX, legendY + row * lineHeight - 12);
        g.setColor(Color.BLACK);
        g.drawString(String.format("Confidence (orientation): ±%.1f° cone",
                        Math.toDegrees(solution.orientationUncertainty)),
                legendX + 22, legendY + row * lineHeight);
        row++;

        // 3) Convergence: converged? and steps
        g.setColor(Color.BLACK);
        g.drawString(String.format("Constraints satisfied: %s", solution.ok ? "Yes" : "No"),
                legendX, legendY + row * lineHeight);
        row++;

        // Keep context for inputs (optional, below the requested items)
        row++;
        g.setColor(Color.RED); g.fillOval(legendX, legendY + row*lineHeight - 6, 12, 12);
        g.setColor(Color.BLACK); g.drawString("Odometry Prior", legendX + 22, legendY + row*lineHeight); row++;
        g.setColor(Color.BLUE); g.fillOval(legendX, legendY + row*lineHeight - 6, 12, 12);
        g.setColor(Color.BLACK); g.drawString("Location Detection", legendX + 22, legendY + row*lineHeight); row++;
        g.setColor(new Color(0, 180, 0)); g.fillOval(legendX, legendY + row*lineHeight - 6, 12, 12);
        g.setColor(Color.BLACK); g.drawString("YOLO Landmark", legendX + 22, legendY + row*lineHeight);

        // Footer stats (compact)
        int infoY = height - 80;
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(Color.BLACK);
        g.drawString(String.format("Solution: (%.2f, %.2f) | %.1f°",
                solution.x, solution.y, solution.theta * 180.0 / Math.PI), 20, infoY);
        g.drawString(String.format("Confidence: ellipse ~95%% | ±%.1f°",
                Math.toDegrees(solution.orientationUncertainty)), 20, infoY + 20);
        g.drawString(String.format("Constraints satisfied: %s", solution.ok ? "Yes" : "No"), 20, infoY + 40);

        // Cleanup
        g.dispose();

        // Save to file
        ImageIO.write(image, "PNG", new File(outputFilePath));
    }

    // === NEW HELPERS ===

    /** Draw the position covariance ellipse (~95%): scale = sqrt(chi2_2,0.95) ≈ 2.4477 */
    private static void drawLocationConfidenceEllipse(Graphics2D g, CoordTransform ct, Solution solution) {
        double[][] C = solution.positionCovariance;
        double a = C[0][0], b = C[0][1], c = C[1][1];

        // Eigen-decomposition for symmetric 2x2
        double trace = a + c;
        double det = a * c - b * b;
        double tmp = Math.sqrt(Math.max(0.0, trace * trace / 4.0 - det));
        double l1 = trace / 2.0 + tmp;  // larger eigenvalue
        double l2 = trace / 2.0 - tmp;

        // Eigenvector for l1
        double vx = (Math.abs(b) > 1e-12) ? (l1 - c) : 1.0;
        double vy = (Math.abs(b) > 1e-12) ? b : 0.0;
        double n = Math.hypot(vx, vy);
        if (n < 1e-12) { vx = 1; vy = 0; n = 1; }
        vx /= n; vy /= n;

        double angle = Math.atan2(vy, vx); // math angle

        // 95% scaling for 2D Gaussian
        final double SCALE95 = Math.sqrt(5.991464547107979); // ≈ 2.4477
        double rx = SCALE95 * Math.sqrt(Math.max(l1, 0));
        double ry = SCALE95 * Math.sqrt(Math.max(l2, 0));

        // Convert to pixels
        double rxPx = ct.toScreenScale(rx);
        double ryPx = ct.toScreenScale(ry);

        double cx = ct.toScreenX(solution.x);
        double cy = ct.toScreenY(solution.y);

        Shape ellipse = new Ellipse2D.Double(-rxPx, -ryPx, 2 * rxPx, 2 * ryPx);
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        at.rotate(-angle); // invert because screen Y increases downward
        Shape transformed = at.createTransformedShape(ellipse);

        // Fill + stroke in semi-transparent black
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(transformed);
        g.setColor(new Color(0, 0, 0, 140));
        g.setStroke(new BasicStroke(2f));
        g.draw(transformed);
    }

    /** Draw an orientation confidence cone in black, centered at solution heading with ±uncertainty */
    private static void drawOrientationConfidenceCone(Graphics2D g, CoordTransform ct, Solution solution,
                                                      double minX, double maxX, double minY, double maxY) {

        int solX = ct.toScreenX(solution.x);
        int solY = ct.toScreenY(solution.y);

        // Make the cone length a fraction of scene size for clarity
        double scene = Math.max(maxX - minX, maxY - minY);
        double length = 0.35 * scene; // 35% of the larger span
        double L = ct.toScreenScale(length);

        double alpha = solution.theta;
        double half = solution.orientationUncertainty;

        double a1 = alpha + half;
        double a2 = alpha - half;

        Path2D.Double cone = new Path2D.Double();
        cone.moveTo(solX, solY);

        int segments = 36;
        for (int j = 0; j <= segments; j++) {
            double ang = a1 + (a2 - a1) * j / segments;
            double px = solX + L * Math.cos(ang);
            double py = solY - L * Math.sin(ang);
            cone.lineTo(px, py);
        }
        cone.closePath();

        g.setColor(new Color(0, 0, 0, 40));
        g.fill(cone);
        g.setColor(new Color(0, 0, 0, 140));
        g.setStroke(new BasicStroke(2f));
        g.draw(cone);

        // Center ray (dashed)
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 6}, 0));
        int cx = (int) (solX + L * Math.cos(alpha));
        int cy = (int) (solY - L * Math.sin(alpha));
        g.drawLine(solX, solY, cx, cy);
        g.setStroke(old);
    }

    /** Tiny wedge for legend entry of orientation confidence */
    private static void drawMiniConeKey(Graphics2D g, int x, int y) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x, y + 7);
        p.lineTo(x + 18, y);
        p.lineTo(x + 18, y + 14);
        p.closePath();
        g.setColor(new Color(0, 0, 0, 40)); g.fill(p);
        g.setColor(new Color(0, 0, 0, 140)); g.setStroke(new BasicStroke(2f)); g.draw(p);
    }

    /**
     * Draw an arrow at given position with specified angle
     */
    private static void drawArrow(Graphics2D g, int x, int y, double angle, int length, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(3));

        // Arrow end point
        int endX = x + (int)(length * Math.cos(angle));
        int endY = y - (int)(length * Math.sin(angle));  // minus because screen Y is inverted

        // Draw main line
        g.drawLine(x, y, endX, endY);

        // Draw arrowhead
        double arrowAngle = 0.5;  // radians
        int arrowLength = length / 4;

        int arrow1X = endX - (int)(arrowLength * Math.cos(angle - arrowAngle));
        int arrow1Y = endY + (int)(arrowLength * Math.sin(angle - arrowAngle));

        int arrow2X = endX - (int)(arrowLength * Math.cos(angle + arrowAngle));
        int arrow2Y = endY + (int)(arrowLength * Math.sin(angle + arrowAngle));

        g.drawLine(endX, endY, arrow1X, arrow1Y);
        g.drawLine(endX, endY, arrow2X, arrow2Y);
    }

    /**
     * Normalize angle to [-pi, pi]
     */
    private static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
}
 