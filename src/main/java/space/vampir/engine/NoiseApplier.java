package space.vampir.engine;

import space.vampir.engine.message.Imu;
import space.vampir.engine.message.NavSat;
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
                odometry.getTheta() + thetaNoise,
                odometry.getUncertaintyInMeters());
    }

    /**
     * Adds random noise to the given navSat. The standard deviations are expected to be in the same units as the navSat.
     *
     * @param navSat     the navSat to which noise should be added
     * @param radiusStdDev the standard deviation of the noise of the radius (distance from the original position)
     * @return a new navSat with added noise, or null if the input navSat is null
     */
    public static NavSat addNoise(NavSat navSat, double radiusStdDev) {
        if (navSat == null) {
            return null;
        }

        double r = random.nextGaussian(0.0, radiusStdDev);
        double angle = random.nextDouble(0, 2 * Math.PI);
        double xNoise = r * Math.cos(angle); // in meters
        double yNoise = r * Math.sin(angle); // in meters

        double xLatNoise = GeoUtilsApprox.metersToLatitudeDegrees(xNoise);
        double yLonNoise = GeoUtilsApprox.metersToLongitudeDegrees(yNoise, navSat.getLat());

        return new NavSat(
                navSat.getTime(),
                navSat.getLat() + xLatNoise,
                navSat.getLon() + yLonNoise,
                navSat.getPositionCovariance());
    }

    /**
     * Adds random noise to the given imu. The standard deviations are expected to be in the same units as the imu.
     *
     * @param thetaStdDev the standard deviation of the noise of the angle (orientation)
     * @return a new imu with added noise, or null if the input imu is null
     */
    public static Imu addNoise(Imu imu, double thetaStdDev) {
        if (imu == null) {
            return null;
        }

        double thetaNoise = random.nextGaussian(0.0, thetaStdDev);

        return new Imu(
                imu.getTime(),
                imu.getTheta() + thetaNoise);
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
