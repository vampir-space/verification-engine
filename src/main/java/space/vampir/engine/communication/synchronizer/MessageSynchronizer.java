package space.vampir.engine.communication.synchronizer;

import space.vampir.engine.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MessageSynchronizer {

    protected final long maxTimeDifference;
    private final List<String> requiredTopics;
    protected final List<Integer> requiredTopicIndices = new ArrayList<>();

    protected MessageSynchronizer(long maxTimeDifference, List<String> requiredTopics) {
        this.maxTimeDifference = maxTimeDifference;
        this.requiredTopics = requiredTopics;
    }

    public void setTopicIndices(Map<String, Integer> topicIndices) {
        for (String requiredTopic : requiredTopics) {
            Integer index = topicIndices.get(requiredTopic);
            if (index != null) {
                requiredTopicIndices.add(index);
            }
        }
    }

    /**
     * Returns the indices of the messages in the message queues that are synchronized. The order of
     * the indices corresponds to the order of the message queues. An index -1 is used to indicate
     * that no message from the corresponding queue is part of the synchronization.
     *
     * @return the indices of the synchronized messages in the message queues or null if no
     * synchronization is possible (e.g., missing required messages)
     */
    public abstract int[] getSynchronizedMessageIndices(List<List<? extends Message>> messageQueues);
}
