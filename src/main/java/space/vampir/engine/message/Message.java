package space.vampir.engine.message;

import java.util.List;
import java.util.Map;

public abstract class Message {
    long time;

    public Message(long time) {
        this.time = time;
    }

    public static long readTime(Object map) {
        var timeStamp = readMap(readMap(map, "header"), "stamp");
        int sec = (int) readMap(timeStamp, "sec");
        int nano = (int) readMap(timeStamp, "nanosec");
        return sec * 1000000000L + nano;
    }

    protected static Object readMap(Object map, String key) {
        Map<String, Object> map2 = (Map<String, Object>) map;
        return map2.get(key);
    }

    protected static List<Object> readArray(Object map, String key) {
        Map<String, Object> map2 = (Map<String, Object>) map;
        return (List<Object>) map2.get(key);
    }

    public long getTime() {
        return time;
    }
}
