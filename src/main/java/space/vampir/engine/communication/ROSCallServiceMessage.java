package space.vampir.engine.communication;

public class ROSCallServiceMessage {
    public String op;
    public String id;
    public String service;   // for call_service / service_response
    public Object args;      // for call_service


    // constructor for call_service
    public ROSCallServiceMessage(String op, String service, Object args, String id) {
        this.op = op;
        this.service = service;
        this.args = args;
        this.id = id;
    }
}
