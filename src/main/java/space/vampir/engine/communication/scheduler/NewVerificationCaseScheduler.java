package space.vampir.engine.communication.scheduler;

import space.vampir.engine.communication.StateRecorder;

public interface NewVerificationCaseScheduler {

    boolean shouldScheduleNewVerificationCase(StateRecorder stateRecorder);
}
