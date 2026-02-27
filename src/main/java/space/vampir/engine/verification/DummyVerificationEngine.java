package space.vampir.engine.verification;

import space.vampir.engine.NoiseApplier;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public class DummyVerificationEngine implements VerificationEngine {

    private final double noise;

    public DummyVerificationEngine(double noise) {
        this.noise = noise;
    }

    @Override
    public UpdatedScenario update(Scenario scenario) {
        Odometry groundTruth = scenario.odometry();
        if (groundTruth == null) {
            return new UpdatedScenario(scenario, null, null);
        }
        Odometry newValue = NoiseApplier.addNoise(groundTruth, noise);
        return new UpdatedScenario(scenario, newValue, groundTruth);
    }
}
