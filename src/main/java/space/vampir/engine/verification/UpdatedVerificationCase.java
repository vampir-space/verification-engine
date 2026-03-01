package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public record UpdatedVerificationCase(
        UpdatedScenario updatedScenario,
        Odometry groundTruth
) {
    public Scenario scenario() {
        return updatedScenario.scenario();
    }

    public Odometry updatedByVerificationEngine() {
        return updatedScenario.updatedByVerificationEngine();
    }
}
