package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

import java.util.List;

public record UpdatedScenario(
        Scenario scenario,
        Odometry updatedByVerificationEngine,
        int numberOfLandmarks,
        List<String> associations) {
    public boolean use() {
        return updatedByVerificationEngine != null;
    }
}
