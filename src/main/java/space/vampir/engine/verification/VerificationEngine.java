package space.vampir.engine.verification;

import space.vampir.engine.message.Scenario;

public interface VerificationEngine {
    UpdatedScenario update(Scenario scenario);

    default UpdatedScenario update(UpdatedScenario scenario) {
        if (scenario.updatedByVerificationEngine() != null) {
            throw new IllegalStateException("The scenario has already been updated by a verification engine.");
        }
        UpdatedScenario updatedScenario = update(scenario.scenario());
        return new UpdatedScenario(
                updatedScenario.scenario(),
                updatedScenario.updatedByVerificationEngine(),
                scenario.groundTruth()
        );
    }
}
