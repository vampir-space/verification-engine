package space.vampir.engine.communication;

public class ROSSubscribeMessage {
    public final String op = "subscribe";
    public final String id;
    public final String topic;     // for subscribe/publish
    public final String type;      // for subscribe

    // constructor for subscribe
    public ROSSubscribeMessage(String topic, String type, String id) {
        this.topic = topic;
        this.type = type;
        this.id = id;
    }
}
