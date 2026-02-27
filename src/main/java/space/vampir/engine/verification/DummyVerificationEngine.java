package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public class DummyVerificationEngine implements VerificationEngine {

    private final double gnssNoise;
    private final double verificationEngineNoise;

    public DummyVerificationEngine(double gnssNoise, double verificationEngineNoise) {
        this.gnssNoise = gnssNoise;
        this.verificationEngineNoise = verificationEngineNoise;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        Odometry groundTruth = rawScenario.odometry();
        if (groundTruth == null) {
            return new UpdatedScenario(rawScenario, null, null);
        }
        Odometry gnssValue = addNoise(groundTruth, gnssNoise);
        Odometry newValue = addNoise(groundTruth, verificationEngineNoise);
        Scenario newRawScenario = new Scenario(
                rawScenario.time(),
                gnssValue,
                rawScenario.pointPillars(),
                rawScenario.yolo()
        );
        return new UpdatedScenario(newRawScenario, newValue, groundTruth);
    }

    private Odometry addNoise(Odometry odometry, double noise) {
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
