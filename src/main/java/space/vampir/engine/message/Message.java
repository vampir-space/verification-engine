package space.vampir.engine.message;

import java.util.Map;

public abstract class Message {
    long time;

    Message(long time) {
        this.time = time;
    }

    protected static Object readMap(Object map, String key) {
        Map<String, Object> map2 = (Map<String, Object>) map;
        return map2.get(key);
    }

    public long getTime() {
        return time;
    }
}
