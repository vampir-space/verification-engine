package space.vampir.engine.communication;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Records ROS messages to a single JSON Lines file.
 * Each line is a JSON object: {seq: long, topic: string, time: long|null, msg: object}
 * <p>
 * Thread-safe via synchronized write operations.
 * </p>
 */
public class JsonlMessageRecorder implements MessageRecorder {
    private final BufferedWriter writer;
    private final ObjectMapper mapper;
    private final AtomicLong sequenceCounter = new AtomicLong(0);

    /**
     * Create a JSONL recorder writing to the given file.
     * If the file does not exist, it will be created.
     * If it exists, new records are appended.
     *
     * @param file the output JSON Lines file
     * @throws IOException if the file cannot be opened
     */
    public JsonlMessageRecorder(File file) throws IOException {
        this.mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.writer = new BufferedWriter(new FileWriter(file, true)); // append mode
    }

    @Override
    public synchronized void record(String topic, Object msg, Long timestamp) {
        try {
            long seq = sequenceCounter.getAndIncrement();
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("seq", seq);
            record.put("topic", topic);
            record.put("time", timestamp);
            record.put("msg", msg);

            String jsonLine = mapper.writeValueAsString(record);
            writer.write(jsonLine);
            writer.write("\n");
        } catch (JsonProcessingException e) {
            System.err.println("Error serializing message for JSONL: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error writing to JSONL file: " + e.getMessage());
        }
    }

    /**
     * Flush any buffered data to disk without closing the writer.
     */
    public synchronized void flush() {
        try {
            if (writer != null) {
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error flushing JSONL recorder: " + e.getMessage());
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing JSONL recorder: " + e.getMessage());
        }
    }
}

