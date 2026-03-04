package space.vampir.engine.communication.scheduler;

import space.vampir.engine.communication.StateRecorder;

public class AlwaysScheduler implements NewVerificationCaseScheduler {
    @Override
    public boolean shouldScheduleNewVerificationCase(StateRecorder stateRecorder) {
        return true;
    }
}
