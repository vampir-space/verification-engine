package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public record VerificationCase(
        Scenario scenario,
        Odometry groundTruth
) {
    public UpdatedVerificationCase update(UpdatedScenario updatedScenario) {
        return new UpdatedVerificationCase(updatedScenario, groundTruth);
    }
}
