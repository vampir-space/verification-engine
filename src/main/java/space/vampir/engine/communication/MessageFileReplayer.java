package space.vampir.engine.communication;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Replays ROS messages from recorded files (JSONL or per-topic CSV).
 * Forwards messages to a StateRecorder in the order they were originally received.
 */
public class MessageFileReplayer {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Replay messages from a JSON Lines file.
     * Each line should be a JSON object with {seq, topic, time, msg}.
     * Messages are replayed in file order (which is arrival order).
     *
     * @param jsonlFile the JSONL file to replay
     * @param callback  called for each message: callback.accept(topic, msg)
     * @throws IOException if the file cannot be read
     */
    public void replayJsonl(File jsonlFile, MessageCallback callback) throws IOException {
        System.out.println("Replaying from JSONL: " + jsonlFile.getAbsolutePath());

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonlFile))) {
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = mapper.readValue(line, Map.class);
                    String topic = (String) record.get("topic");
                    Object msg = record.get("msg");

                    if (topic == null || msg == null) {
                        System.err.println("Warning: skipping line " + lineNumber + " with missing topic or msg");
                        continue;
                    }

                    callback.accept(topic, msg);
                } catch (Exception e) {
                    e.printStackTrace();
                    //e.getMessage();
                    //System.err.println("Warning: error parsing JSONL line " + lineNumber + ": " + e.getMessage());
                }
            }
        }

        System.out.println("✓ Finished replaying JSONL");
    }

    /**
     * Replay messages from per-topic CSV files in a directory.
     * CSV files should have columns: global_seq, timestamp, topic, topic_seq, payload_json.
     * Messages are replayed in global_seq order (cross-topic ordering).
     *
     * @param csvDirectory directory containing per-topic CSV files
     * @param callback     called for each message: callback.accept(topic, msg)
     * @throws IOException if the directory cannot be read or CSV files cannot be parsed
     */
    public void replayPerTopicCsv(File csvDirectory, MessageCallback callback) throws IOException {
        System.out.println("Replaying from per-topic CSV: " + csvDirectory.getAbsolutePath());

        List<CsvEntry> allEntries = new ArrayList<>();

        // Read all CSV files from the directory
        try (Stream<Path> paths = Files.list(csvDirectory.toPath())) {
            paths.filter(p -> p.toString().endsWith(".csv"))
                    .forEach(csvPath -> {
                        try {
                            readCsvFile(csvPath.toFile(), allEntries);
                        } catch (IOException e) {
                            System.err.println("Warning: error reading CSV file " + csvPath.getFileName() + ": " + e.getMessage());
                        }
                    });
        }

        // Sort by global_seq to restore arrival order
        allEntries.sort(Comparator.comparingLong(e -> e.globalSeq));

        // Replay in order
        for (CsvEntry entry : allEntries) {
            try {
                callback.accept(entry.topic, entry.msg);
            } catch (Exception e) {
                System.err.println("Warning: error replaying message: " + e.getMessage());
            }
        }

        System.out.println("✓ Finished replaying " + allEntries.size() + " messages from per-topic CSV");
    }

    /**
     * Read a single CSV file and extract entries.
     */
    private void readCsvFile(File csvFile, List<CsvEntry> entries) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    // Skip header
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                try {
                    CsvEntry entry = parseCsvLine(line);
                    if (entry != null) {
                        entries.add(entry);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: error parsing CSV line " + lineNumber + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Parse a CSV line: global_seq,timestamp,topic,topic_seq,payload_json
     * payload_json is quoted and may contain commas/quotes, so extract carefully.
     */
    private CsvEntry parseCsvLine(String line) throws IOException {
        // Find the last double-quote to locate the JSON payload
        int lastQuote = line.lastIndexOf('"');
        if (lastQuote < 0) {
            return null; // malformed
        }

        String prefix = line.substring(0, line.indexOf('"'));
        String jsonPart = line.substring(line.indexOf('"') + 1, lastQuote);

        // Parse prefix: global_seq,timestamp,topic,topic_seq
        String[] parts = prefix.split(",");
        if (parts.length < 4) {
            return null;
        }

        long globalSeq = Long.parseLong(parts[0].trim());
        String topic = parts[2].trim(); // Topic is the third field

        // Unescape CSV-escaped quotes
        String unescapedJson = jsonPart.replace("\"\"", "\"");

        // Parse payload JSON
        @SuppressWarnings("unchecked")
        Map<String, Object> msg = mapper.readValue(unescapedJson, Map.class);

        return new CsvEntry(globalSeq, topic, msg);
    }


    /**
     * Callback interface for replayed messages.
     */
    @FunctionalInterface
    public interface MessageCallback {
        void accept(String topic, Object msg) throws Exception;
    }

    /**
     * Internal class for holding a CSV entry.
     */
    private static class CsvEntry {
        final long globalSeq;
        final String topic;
        final Map<String, Object> msg;

        CsvEntry(long globalSeq, String topic, Map<String, Object> msg) {
            this.globalSeq = globalSeq;
            this.topic = topic;
            this.msg = msg;
        }
    }
}

