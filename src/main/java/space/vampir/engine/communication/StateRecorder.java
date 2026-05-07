package space.vampir.engine.communication;

import space.vampir.engine.communication.scheduler.NewVerificationCaseScheduler;
import space.vampir.engine.communication.synchronizer.MessageSynchronizer;
import space.vampir.engine.message.Message;
import space.vampir.engine.message.NavSat;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.PointPillars;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.verification.VerificationCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StateRecorder {

    public static class TopicStatistics {
        long minTime = 0;
        long maxTime = 0;
        long count = 0;
    }

    public record SynchronizedMessages(
            Odometry odometry,
            Odometry lowEndOdometry,
            PointPillars pointPillars,
            Yolo yolo,
            NavSat navSat
    ) {
    }

    public static final String syncedTopic = "/synchronized_messages";
    public static final String odometryTopic = "/ground_truth/odometry";
    public static final String lowEndOdometryTopic = "/ground_truth/odometry_ublox";
    public static final String pointPillarsTopic = "/detections/pointpillars";
    public static final String yoloTopic = "/detections/yolo";
    public static final String navSatTopic = "/simulated_navsat_data";

    public final Map<String, Integer> topicIndices = new LinkedHashMap<>();
    public final Map<String, TopicStatistics> topicStatistics = new LinkedHashMap<>();

    //public static final String imageTopic = "/sensor/image";
    //public static final Set<String> extraTopics = Set.of(imageTopic);

    public final List<List<? extends Message>> messageQueues = new ArrayList<>();

    private final List<Odometry> odometries = new ArrayList<>();
    private final int odometryMessageQueueIndex = addMessageQueue(odometryTopic, odometries);

    private final List<Odometry> lowEndOdometries = new ArrayList<>();
    private final int lowEndOdometryMessageQueueIndex = addMessageQueue(lowEndOdometryTopic, lowEndOdometries);

    private final List<PointPillars> pointPillars = new ArrayList<>();
    private final int pointPillarsMessageQueueIndex = addMessageQueue(pointPillarsTopic, pointPillars);

    private final List<Yolo> yolos = new ArrayList<>();
    private final int yoloMessageQueueIndex = addMessageQueue(yoloTopic, yolos);

    private final List<NavSat> navsats = new ArrayList<>();
    private final int navSatMessageQueueIndex = addMessageQueue(navSatTopic, navsats);

    private final StateListener listener;
    private final VerificationCaseProvider verificationCaseProvider;
    private final NewVerificationCaseScheduler newVerificationCaseScheduler;
    private final MessageSynchronizer messageSynchronizer;

    private final Object synchronizationLock = new Object();
    private long lastVerificationCaseTime = -1;

    private final long minWaitTime;
    private final long dropOlderThan;

    public StateRecorder(StateListener listener,
                         VerificationCaseProvider verificationCaseProvider,
                         NewVerificationCaseScheduler newVerificationCaseScheduler,
                         MessageSynchronizer messageSynchronizer,
                         long minWaitTime,
                         long dropOlderThan) {
        this.listener = listener;
        this.verificationCaseProvider = verificationCaseProvider;
        this.newVerificationCaseScheduler = newVerificationCaseScheduler;
        this.messageSynchronizer = messageSynchronizer;
        messageSynchronizer.setTopicIndices(topicIndices);
        this.minWaitTime = minWaitTime;
        this.dropOlderThan = dropOlderThan;
    }

    private int addMessageQueue(String topic, List<? extends Message> messageQueue) {
        messageQueues.add(messageQueue);
        int index = messageQueues.size() - 1;
        topicIndices.put(topic, index);
        return index;
    }

    public synchronized void messageReceived(String topic, Object message) {
        Message result = switch (topic) {
            case odometryTopic -> {
                var rawOdometry = Odometry.fromMap(message);
                var newOdometry = new Odometry(rawOdometry.getTime() - 37 * 1000000000L, rawOdometry.getX(), rawOdometry.getY(), -rawOdometry.getTheta() + Math.PI);
                yield insertMessage(odometries, newOdometry);
            }
            case lowEndOdometryTopic -> {
                var odometry = Odometry.fromMap(message);
                yield insertMessage(lowEndOdometries, odometry);
            }
            case pointPillarsTopic -> insertMessage(pointPillars, PointPillars.fromMap(message));
            case yoloTopic -> insertMessage(yolos, Yolo.fromMap(message));
            case navSatTopic -> insertMessage(navsats, NavSat.fromMap(message));
//            case syncedTopic -> {
//                Message res = null;
//                for (var t : messageTopics) {
//                    var m = (Map<String, Object>) message;
//                    if (m.containsKey(t)) {
//                        res = messageReceived(t, m.get(t));
//                    }
//                }
//                yield res;
//            }
            default ->
                    throw new UnsupportedOperationException("Unknown message: " + topic + " > " + message);
        };

        if (result == null) {
            return;
        }

        var stat = topicStatistics.getOrDefault(topic, new TopicStatistics());
        stat.count++;
        stat.minTime = Math.min(stat.minTime, result.getTime());
        stat.maxTime = Math.max(stat.maxTime, result.getTime());
        topicStatistics.put(topic, stat);

        if (result.getTime() - lastVerificationCaseTime > minWaitTime) {
            boolean shouldTryNewVerificationCase;
            synchronized (synchronizationLock) {
//                System.out.println(result.getTime());
                for (var queue : messageQueues) {
//                    for (var m : queue) {
//                        if (m.getTime() < result.getTime() - dropOlderThan) {
//                            System.out.println(m.getTime());
//                            System.out.println(dropOlderThan);
//                            System.out.println((result.getTime() - m.getTime()));
//                        }
//                    }
                    queue.removeIf(m -> m.getTime() < result.getTime() - dropOlderThan);
                }
                shouldTryNewVerificationCase = newVerificationCaseScheduler.shouldScheduleNewVerificationCase(this);
            }

            if (shouldTryNewVerificationCase) {
                tryNewVerificationCase();
            }
        }
    }

    public void tryNewVerificationCase() {
        VerificationCase verificationCase = getCurrentVerificationCase();
        if (verificationCase != null) {
            listener.stateInvalidated(verificationCase);
            lastVerificationCaseTime = verificationCase.scenario().time();
        }
    }

    private VerificationCase getCurrentVerificationCase() {
        SynchronizedMessages sync;
        synchronized (synchronizationLock) {
            int[] messageIndices = messageSynchronizer.getSynchronizedMessageIndices(messageQueues);
            if (messageIndices == null) {
                return null;
            }

            Odometry odometry = retrieveMessage(odometries, messageIndices, odometryMessageQueueIndex);
            Odometry lowEndOdometry = retrieveMessage(lowEndOdometries, messageIndices, lowEndOdometryMessageQueueIndex);
            PointPillars pointPillar = retrieveMessage(pointPillars, messageIndices, pointPillarsMessageQueueIndex);
            Yolo yolo = retrieveMessage(yolos, messageIndices, yoloMessageQueueIndex);
            NavSat navSat = retrieveMessage(navsats, messageIndices, navSatMessageQueueIndex);

            sync = new SynchronizedMessages(odometry, lowEndOdometry, pointPillar, yolo, navSat);
        }

        return verificationCaseProvider.getVerificationCase(sync);
    }

    /**
     * Retrieves the message from the given queue based on the provided indices and queue index. It also removes any messages from the queue that are older than the retrieved message's time.
     */
    private <T extends Message> T retrieveMessage(List<T> queue, int[] indices, int queueIndex) {
        int index = indices[queueIndex];
        if (index >= 0 && index < queue.size()) {
            var message = queue.get(index);
            queue.removeIf(m -> m.getTime() < message.getTime());
            return message;
        }
        return null;
    }

    /**
     * Inserts the given message into the specified queue while maintaining the order based on the message's time (in case messages arrive out of order).
     */
    private <T extends Message> T insertMessage(List<T> queue, T message) {
        if (message == null) {
            return null;
        }
//        System.out.println(message);
        synchronized (synchronizationLock) {
            int index = Collections.binarySearch(queue, message, Comparator.comparingLong(Message::getTime));
            if (index < 0) {
                index = -index - 1;
            }
            queue.add(index, message);
        }
        return message;
    }
}
