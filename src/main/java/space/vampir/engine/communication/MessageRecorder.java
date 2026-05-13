package space.vampir.engine.communication;

/**
 * Interface for recording ROS messages to persistent storage.
 * Implementations may persist data in different formats (JSON Lines, CSV, etc.).
 */
public interface MessageRecorder extends AutoCloseable {
    /**
     * Record a ROS publish event.
     *
     * @param topic     the ROS topic name (e.g., "/ground_truth/odometry")
     * @param msg       the raw ROS message payload as a Map
     * @param timestamp the message timestamp in nanoseconds (may be null if unavailable)
     */
    void record(String topic, Object msg, Long timestamp);

    /**
     * Flush any pending writes
     */
    void flush();

    /**
     * Flush any pending writes and close resources.
     */
    @Override
    void close();
}

