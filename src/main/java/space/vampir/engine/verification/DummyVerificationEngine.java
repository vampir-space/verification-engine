package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public class DummyVerificationEngine implements VerificationEngine{
    final double noise;

    public DummyVerificationEngine(double noise) {
        this.noise = noise;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        Odometry newValue = new Odometry(
                rawScenario.time(),
                rawScenario.odometry().getX()+noise,
                rawScenario.odometry().getY(),
                rawScenario.odometry().getTheta());
        return new UpdatedScenario(rawScenario, newValue,null);
    }
}
