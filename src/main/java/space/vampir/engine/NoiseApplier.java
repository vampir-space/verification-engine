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
     * @param odometry the odometry to which noise should be added
     * @param radiusStdDev the standard deviation of the noise of the radius (distance from the original position)
     * @param thetaStdDev the standard deviation of the noise of the angle (orientation)
     * @return a new odometry with added noise, or null if the input odometry is null
     */
    public static Odometry addNoise(Odometry odometry, double radiusStdDev, double thetaStdDev) {
        if (odometry == null) {
            return null;
        }

        double r = random.nextGaussian(0.0, radiusStdDev);
        double angle = random.nextDouble(0, 2 * Math.PI);
        double xNoise = r * Math.cos(angle);
        double yNoise = r * Math.sin(angle);
        double thetaNoise = random.nextGaussian(0.0, thetaStdDev);

        return new Odometry(
                odometry.getTime(),
                odometry.getX() + xNoise,
                odometry.getY() + yNoise,
                odometry.getTheta() + thetaNoise);
    }
}
