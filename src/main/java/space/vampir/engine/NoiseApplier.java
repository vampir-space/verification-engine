package space.vampir.engine;

import space.vampir.engine.message.Odometry;

public class NoiseApplier {
    public static Odometry addNoise(Odometry odometry, double noise) {
        double noiseRadius = Math.random() * noise;
        double xNoise = Math.random() * noiseRadius * 2 - noiseRadius;
        double yNoise = Math.sqrt(noiseRadius * noiseRadius - xNoise * xNoise);
        return new Odometry(
                odometry.getTime(),
                odometry.getX() + xNoise,
                odometry.getY() + yNoise,
                odometry.getTheta());
    }
}
