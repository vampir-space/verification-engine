package space.vampir.engine.communication.scheduler;

import space.vampir.engine.communication.StateRecorder;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DriveByTopicScheduler implements NewVerificationCaseScheduler {

    private final String topic;
    private final int delay; // ms
    private boolean scheduled = false;

    public DriveByTopicScheduler(String topic, int delay) {
        this.topic = topic;
        this.delay = delay;
    }

    @Override
    public boolean shouldScheduleNewVerificationCase(StateRecorder recorder) {
        if (scheduled || recorder.messageQueues.get(recorder.topicIndices.get(topic)).isEmpty()) {
            return false;
        }
        if (delay == 0) {
            return true;
        }

        scheduled = true;
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                recorder.tryNewVerificationCase();
            } finally {
                scheduled = false;
                scheduler.shutdown();
            }
        }, delay, TimeUnit.MILLISECONDS);

        return false;
    }
}
