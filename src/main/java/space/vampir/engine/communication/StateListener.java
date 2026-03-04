package space.vampir.engine.communication;

import space.vampir.engine.verification.VerificationCase;

public interface StateListener {
    void stateInvalidated(VerificationCase newState);
}
