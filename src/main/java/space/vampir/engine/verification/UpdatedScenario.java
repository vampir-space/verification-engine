package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

public record UpdatedScenario(Scenario scenario, Odometry updatedByVerificationEngine, Odometry groundTruth) {

}
