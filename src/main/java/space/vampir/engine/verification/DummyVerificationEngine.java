package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public class DummyVerificationEngine implements VerificationEngine {

    private static final boolean ADD_GNSS_NOISE = true;

    private final double noise;

    public DummyVerificationEngine(double noise) {
        this.noise = noise;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        Odometry groundTruth = rawScenario.odometry();
        if (groundTruth == null) {
            return new UpdatedScenario(rawScenario, null, null);
        }
        Odometry newValue = addNoise(groundTruth);
        Odometry gnssValue = ADD_GNSS_NOISE ? addNoise(groundTruth) : groundTruth;
        Scenario newRawScenario = new Scenario(
                rawScenario.time(),
                gnssValue,
                rawScenario.pointPillars(),
                rawScenario.yolo()
        );
        return new UpdatedScenario(newRawScenario, newValue, groundTruth);
    }

    private Odometry addNoise(Odometry odometry) {
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
