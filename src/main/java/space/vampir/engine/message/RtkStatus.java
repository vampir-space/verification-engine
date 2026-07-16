package space.vampir.engine.message;

public class RtkStatus extends Message {

    final boolean valid;

    public RtkStatus(long time, boolean valid) {
        super(time);
        this.valid = valid;
    }

    public static RtkStatus fromMap(Object map) {
        long time = readTime(map);
        var valid = (boolean) readMap(map, "valid");
        return new RtkStatus(time, valid);
    }

    public boolean getValid() {
        return valid;
    }
}
