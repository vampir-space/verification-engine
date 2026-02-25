package space.vampir.engine.verification;

import space.vampir.engine.message.Scenario;

public interface VerificationEngine {
    UpdatedScenario update(Scenario rawScenario);
}
