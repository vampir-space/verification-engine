package space.vampir.engine;

import java.util.HashMap;
import java.util.Map;

public class StateRecorder {
    Map<String, Object> topicToMessageMap = new HashMap<>();

    synchronized void messageReceived(String topic, Object message){
        this.topicToMessageMap.put(topic,message);
    }

    synchronized HashMap<String, Object> getLastState() {
        return new HashMap<>(topicToMessageMap);
    }
}
