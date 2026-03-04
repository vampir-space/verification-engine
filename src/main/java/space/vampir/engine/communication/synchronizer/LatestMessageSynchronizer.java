package space.vampir.engine.communication.synchronizer;

import space.vampir.engine.message.Message;

import java.util.List;

public class LatestMessageSynchronizer extends MessageSynchronizer {

    public LatestMessageSynchronizer(long maxTimeDifference, List<String> requiredTopics) {
        super(maxTimeDifference, requiredTopics);
    }

    @Override
    public int[] getSynchronizedMessageIndices(List<List<? extends Message>> messageQueues) {
        long maxTime = Long.MIN_VALUE;
        int[] indices = new int[messageQueues.size()];
        for (int i = 0; i < messageQueues.size(); i++) {
            var queue = messageQueues.get(i);
            if (!queue.isEmpty()) {
                maxTime = Math.max(maxTime, queue.getLast().getTime());
            }
            indices[i] = queue.size() - 1;
        }
        for (int i = 0; i < messageQueues.size(); i++) {
            var queue = messageQueues.get(i);
            if (!queue.isEmpty()) {
                long timeDiff = maxTime - queue.getLast().getTime();
                if (timeDiff > maxTimeDifference) {
                    indices[i] = -1;
                }
            }
            if (indices[i] == -1 && requiredTopicIndices.contains(i)) {
                return null;
            }
        }
        return indices;
    }
}
