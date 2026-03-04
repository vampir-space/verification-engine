package space.vampir.engine.communication.synchronizer;

import space.vampir.engine.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClosestMessageSynchronizer extends MessageSynchronizer {

    private static final double EPSILON = 1e-4;

    private final Map<String, Integer> topicPriorityByName;
    private final Map<Integer, Integer> topicPriority = new LinkedHashMap<>();

    public ClosestMessageSynchronizer(
            long maxTimeDifference,
            List<String> requiredTopics,
            Map<String, Integer> topicPriority
    ) {
        super(maxTimeDifference, requiredTopics);
        this.topicPriorityByName = topicPriority;
    }

    @Override
    public void setTopicIndices(Map<String, Integer> topicIndices) {
        super.setTopicIndices(topicIndices);
        for (Map.Entry<String, Integer> entry : topicPriorityByName.entrySet()) {
            Integer index = topicIndices.get(entry.getKey());
            if (index != null) {
                topicPriority.put(index, entry.getValue());
            }
        }
    }

    @Override
    public int[] getSynchronizedMessageIndices(List<List<? extends Message>> messageQueues) {
        int[] messageIndices = new int[messageQueues.size()];
        for (int i = 0; i < messageIndices.length; i++) {
            messageIndices[i] = messageQueues.get(i).size() - 1;
            if (messageIndices[i] < 0 && requiredTopicIndices.contains(i)) {
                return null; // if a necessary topic has no messages, a verification case cannot be produced
            }
        }

        ArrayList<Integer> sortedIndices = new ArrayList<>(messageIndices.length);
        for (int i = 0; i < messageIndices.length; i++) {
            if (messageIndices[i] >= 0) {
                insert(messageQueues, messageIndices, sortedIndices, i);
            }
        }

        if (sortedIndices.isEmpty()) {
            return messageIndices;
        }

        Candidate best = getBestCandidate(messageQueues, messageIndices, sortedIndices);
        while (!sortedIndices.isEmpty() && messageIndices[sortedIndices.getLast()] > 0) {
            int last = sortedIndices.removeLast();
            messageIndices[last]--;
            if (messageIndices[last] >= 0) {
                insert(messageQueues, messageIndices, sortedIndices, last);
            }

            Candidate newCandidate = getBestCandidate(messageQueues, messageIndices, sortedIndices);
            if (newCandidate != null && newCandidate.betterThan(best)) {
                best = newCandidate;
            }
        }

        return best == null ? null : best.messageIndices;
    }

    private record Candidate(int[] messageIndices, double priority, long diff) {
        boolean betterThan(Candidate other) {
            return other == null || this.priority > other.priority + EPSILON || (Math.abs(this.priority - other.priority) <= EPSILON && this.diff < other.diff);
        }
    }

    private void insert(List<List<? extends Message>> q, int[] indices, ArrayList<Integer> sortedIndices, int element) {
        int index = Collections.binarySearch(sortedIndices, element, (i1, i2) -> Long.compare(getTime(q, indices, i1), getTime(q, indices, i2)));
        if (index < 0) {
            index = -index - 1;
        }
        sortedIndices.add(index, element);
    }

    private long getTime(List<List<? extends Message>> messageQueues, int[] messageIndices, int i) {
        return messageQueues.get(i).get(messageIndices[i]).getTime();
    }

    private Candidate getBestCandidate(List<List<? extends Message>> q, int[] indices, ArrayList<Integer> sortedIndices) {
        int firstIndex = 0;
        int lastIndex = sortedIndices.size() - 1;
        while (getTime(q, indices, sortedIndices.get(lastIndex)) - getTime(q, indices, sortedIndices.get(firstIndex)) > maxTimeDifference) {
            firstIndex++;
        }

        Candidate best = null;

        while (firstIndex >= 0) {
            int[] actualIndices = Arrays.copyOf(indices, indices.length);
            double priority = 0;
            boolean acceptable = true;
            // mark messages that are not part of the synchronization and compute value of the synchronization
            for (int i = 0; i < sortedIndices.size(); ++i) {
                if (i >= firstIndex && i <= lastIndex) {
                    priority += 1.0 / topicPriority.getOrDefault(sortedIndices.get(i), 1);
                } else {
                    if (requiredTopicIndices.contains(sortedIndices.get(i))) {
                        acceptable = false;
                        break;
                    }
                    actualIndices[sortedIndices.get(i)] = -1;
                }
            }

            if (acceptable) {
                long diff = getTime(q, indices, sortedIndices.get(lastIndex)) - getTime(q, indices, sortedIndices.get(firstIndex));
                assert diff <= maxTimeDifference;
                Candidate newCandidate = new Candidate(actualIndices, priority, diff);
                if (newCandidate.betterThan(best)) {
                    best = newCandidate;
                }
            }

            firstIndex--;
            if (firstIndex < 0) {
                break;
            }
            while (getTime(q, indices, sortedIndices.get(lastIndex)) - getTime(q, indices, sortedIndices.get(firstIndex)) > maxTimeDifference) {
                lastIndex--;
            }
        }

        return best;
    }
}
