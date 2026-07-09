package space.vampir.engine.verification;

import space.vampir.engine.NoiseApplier;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public class DummyVerificationEngine implements VerificationEngine {

    private final double radiusStdDev;
    private final double angleStdDev;

    public DummyVerificationEngine(double radiusStdDev, double angleStdDev) {
        this.radiusStdDev = radiusStdDev;
        this.angleStdDev = angleStdDev;
    }

    @Override
    public UpdatedScenario update(Scenario scenario) {
        Odometry odometry = scenario.odometry();
        if (odometry == null) {
            return new UpdatedScenario(scenario, null,0);
        }
        Odometry newValue = NoiseApplier.addNoise(odometry, radiusStdDev, angleStdDev);
        return new UpdatedScenario(scenario, newValue,0);
    }
}
