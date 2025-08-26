package space.vampir.engine.communication;

import space.vampir.engine.message.*;

import java.util.*;

public class StateRecorder {
    public static final String odometryTopic = "/ground_truth/odometry";
    public static final String pointPillarsTopic = "/detections/pointpillars";
    public static final String yoloTopic = "/detections/yolo";

    public static final Set<String> messageTopics = Set.of(odometryTopic,pointPillarsTopic,yoloTopic);

    final StateListener listener;
    List<Odometry> odometries = new ArrayList<>();
    List<PointPillars> pointPillars = new ArrayList<>();
    List<Yolo> yolos = new ArrayList<>();

    public StateRecorder(StateListener listener) {
        this.listener = listener;
    }

    synchronized void messageReceived(String topic, Object message) {
        if (topic.equals(odometryTopic)) {
            odometries.add(Odometry.fromMap(message));
        } else if(topic.equals(pointPillarsTopic)) {
            pointPillars.add(PointPillars.fromMap(message));
            listener.stateInvalidated(this);
        } else if(topic.equals(yoloTopic)) {
            yolos.add(Yolo.fromMap(message));
        } else {
            throw new UnsupportedOperationException("Unknown message: " + topic + " > " + message);
        }
    }

    public synchronized Scenario getLastState() {
        if (!hasMessage(this.odometries, this.pointPillars, this.yolos)) return null;
        var latestTime = getLatestTime(this.odometries, this.pointPillars, this.yolos);
        var commonTime = getCommonTimeWithMostMessages(latestTime, this.odometries, this.pointPillars, this.yolos);

        return new Scenario(
                getClosest(commonTime,odometries),
                getClosest(commonTime,pointPillars),
                getClosest(commonTime,yolos));
    }

    private boolean hasMessage(List<? extends Message>... messages) {
        for (var message : messages) {
            if (message != null) return true;
        }
        return false;
    }

    long timeLimit = 1000000000 / 2;

    private long getLatestTime(List<? extends Message>... messageQueues) {
        long max = Long.MIN_VALUE;
        for (var queue : messageQueues) {
            if(!queue.isEmpty()) {
                var last = queue.getLast();
                if (last != null) {
                    max = Math.max(last.getTime(), max);
                }
            }
        }
        return max;
    }

    private long getCommonTimeWithMostMessages(long latest, List<? extends Message>... messageQueues) {
        long commonTime = latest;
        for (var queue : messageQueues) {
            if(!queue.isEmpty()) {
                var last = queue.getLast();
                if (last != null && last.getTime() >= latest - timeLimit) {
                    commonTime = Math.min(commonTime, last.getTime());
                }
            }
        }
        return commonTime;
    }

    private <T extends Message> T getClosest(long time, List<? extends T> messageQueue) {
        T last = null;
        for(int i = messageQueue.size()-1; i>=0; i--) {
            if(time < messageQueue.get(i).getTime()) {
                last = messageQueue.get(i);
            } else if(time ==messageQueue.get(i).getTime()) {
                return messageQueue.get(i);
            } else {
                return last;
            }
        }
        return last;
    }
}
