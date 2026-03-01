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
        Odometry odometry = scenario.odometry();
        if (odometry == null) {
            return new UpdatedScenario(scenario, null);
        }
        Odometry newValue = NoiseApplier.addNoise(odometry, noise);
        return new UpdatedScenario(scenario, newValue);
    }
}
