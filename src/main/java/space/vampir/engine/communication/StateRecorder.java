package space.vampir.engine.communication;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;

import java.util.ArrayList;
import java.util.List;

public class StateRecorder {
    final StateListener listener;
    List<Odometry> odometries = new ArrayList<>();

    public StateRecorder(StateListener listener) {
        this.listener = listener;
    }

    synchronized void messageReceived(String topic, Object message){
        if(topic.equals("/ground_truth/odometry")) {
            odometries.add(Odometry.fromMap(message));
        } else {
            throw new UnsupportedOperationException("Unknown message: " + topic + " > " + message);
        }
        listener.stateInvalidated(this);
    }

    public synchronized Scenario getLastState() {
        return new Scenario(odometries.getLast().getTime(), odometries.getLast());
    }
}
