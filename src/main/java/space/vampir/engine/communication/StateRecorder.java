package space.vampir.engine.communication;

import space.vampir.engine.communication.scheduler.NewVerificationCaseScheduler;
import space.vampir.engine.communication.synchronizer.MessageSynchronizer;
import space.vampir.engine.message.*;
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
            NavSat groundTruthGps,
            NavSat lowEndGps,
            Imu imu,
            PointPillars pointPillars,
            Yolo yolo
    ) {
    }

    public static final String syncedTopic = "/synchronized_messages";
    //    public static final String odometryTopic = "/ground_truth/odometry";
//    public static final String lowEndOdometryTopic = "/ground_truth/odometry_ublox";
    public static final String groundTruthGpsTopic = "/ground_truth/gps";
    public static final String lowEndGpsTopic = "/ground_truth/gps_ublox";
    public static final String imuTopic = "/ground_truth/imu";
    public static final String pointPillarsTopic = "/detections/pointpillars";
    public static final String yoloTopic = "/detections/yolo";
    public static final String groundTruthSimNavSatTopic = "/ground_truth/GPS_navsatfix";
    public static final String simNavSatTopic = "/simulated_navsat_data";

    public final Map<String, Integer> topicIndices = new LinkedHashMap<>();
    public final Map<String, TopicStatistics> topicStatistics = new LinkedHashMap<>();

    public final List<List<? extends Message>> messageQueues = new ArrayList<>();

    private final List<NavSat> groundTruthGPSs = new ArrayList<>();
    private final int groundTruthGpsMessageQueueIndex = addMessageQueue(groundTruthGpsTopic, groundTruthGPSs);

    private final List<NavSat> lowEndGPSs = new ArrayList<>();
    private final int lowEndGpsMessageQueueIndex = addMessageQueue(lowEndGpsTopic, lowEndGPSs);

    private final List<Imu> imus = new ArrayList<>();
    private final int imuMessageQueueIndex = addMessageQueue(imuTopic, imus);

    private final List<PointPillars> pointPillars = new ArrayList<>();
    private final int pointPillarsMessageQueueIndex = addMessageQueue(pointPillarsTopic, pointPillars);

    private final List<Yolo> yolos = new ArrayList<>();
    private final int yoloMessageQueueIndex = addMessageQueue(yoloTopic, yolos);

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
//        System.out.println(topic + ": " + message);
        Message result = switch (topic) {
            case groundTruthGpsTopic -> insertMessage(groundTruthGPSs, NavSat.fromMap(message));
            case lowEndGpsTopic -> insertMessage(lowEndGPSs, NavSat.fromMap(message));
            case imuTopic -> insertMessage(imus, Imu.fromMap(message));
            case pointPillarsTopic -> insertMessage(pointPillars, PointPillars.fromMap(message));
            case yoloTopic -> insertMessage(yolos, Yolo.fromMap(message));
            case groundTruthSimNavSatTopic -> insertMessage(groundTruthGPSs, NavSat.fromMap(message));
            case simNavSatTopic -> insertMessage(lowEndGPSs, NavSat.fromMap(message));
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

            NavSat groundTruthGps = retrieveMessage(groundTruthGPSs, messageIndices, groundTruthGpsMessageQueueIndex);
            NavSat lowEndGps = retrieveMessage(lowEndGPSs, messageIndices, lowEndGpsMessageQueueIndex);
            Imu imu = retrieveMessage(imus, messageIndices, imuMessageQueueIndex);
            PointPillars pointPillar = retrieveMessage(pointPillars, messageIndices, pointPillarsMessageQueueIndex);
            Yolo yolo = retrieveMessage(yolos, messageIndices, yoloMessageQueueIndex);

            sync = new SynchronizedMessages(groundTruthGps, lowEndGps, imu, pointPillar, yolo);
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
