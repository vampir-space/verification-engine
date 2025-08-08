package space.vampir.engine;

public class ROSSubscribeMessage {
    public String op;
    public String id;
    public String topic;     // for subscribe/publish
    public String type;      // for subscribe

    // constructor for subscribe
    public ROSSubscribeMessage(String op, String topic, String type, String id) {
        this.op = op;
        this.topic = topic;
        this.type = type;
        this.id = id;
    }
}
