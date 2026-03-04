package space.vampir.engine;

import space.vampir.engine.message.Odometry;

import java.util.Random;

public class NoiseApplier {

    private static final Random random = new Random();

    public static double addGaussianNoise(double value, double stdDev) {
        return value + random.nextGaussian(0.0, stdDev);
    }

    /**
     * Adds random noise to the given odometry. The standard deviations are expected to be in the same units as the odometry.
     *
     * @param odometry     the odometry to which noise should be added
     * @param radiusStdDev the standard deviation of the noise of the radius (distance from the original position)
     * @param thetaStdDev  the standard deviation of the noise of the angle (orientation)
     * @return a new odometry with added noise, or null if the input odometry is null
     */
    public static Odometry addNoise(Odometry odometry, double radiusStdDev, double thetaStdDev) {
        if (odometry == null) {
            return null;
        }

        double r = random.nextGaussian(0.0, radiusStdDev);
        double angle = random.nextDouble(0, 2 * Math.PI);
        double xNoise = r * Math.cos(angle); // in meters
        double yNoise = r * Math.sin(angle); // in meters
        double thetaNoise = random.nextGaussian(0.0, thetaStdDev);

        double xLatNoise = GeoUtilsApprox.metersToLatitudeDegrees(xNoise);
        double yLonNoise = GeoUtilsApprox.metersToLongitudeDegrees(yNoise, odometry.getX());

        return new Odometry(
                odometry.getTime(),
                odometry.getX() + xLatNoise,
                odometry.getY() + yLonNoise,
                odometry.getTheta() + thetaNoise);
    }

    public static final class GeoUtilsApprox {

        private static final double EARTH_RADIUS = 6371008.8; // mean Earth radius (m)

        private GeoUtilsApprox() {
        }

        public static double metersToLatitudeDegrees(double meters) {
            return Math.toDegrees(meters / EARTH_RADIUS);
        }

        public static double metersToLongitudeDegrees(double meters, double latitudeDeg) {
            double latRad = Math.toRadians(latitudeDeg);
            return Math.toDegrees(meters / (EARTH_RADIUS * Math.cos(latRad)));
        }
    }
}
