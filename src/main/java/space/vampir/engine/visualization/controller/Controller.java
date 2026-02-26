package space.vampir.engine.visualization.controller;

import javax.swing.Timer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Controller {

    public enum PlayState {
        PLAY_BACKWARD, PAUSED, PLAY_FORWARD
    }

    private final List<Long> timestamps = new ArrayList<>();
    private final List<ControllerObserver> observers = new CopyOnWriteArrayList<>();

    private int currentIndex = 0;
    private PlayState playState = PlayState.PAUSED;

    private int speed = 0; // 0 = 1x, negative slower, positive faster
    private final int speedMin = -5;
    private final int speedMax = 5;
    private boolean liveMode = true;

    private final Timer playbackTimer;

    public Controller() {
        playbackTimer = new Timer(computeDelayMs(), e -> step());
    }

    /* ---------------- Public API ---------------- */

    public void setTimestamps(List<Long> newTimestamps) {
        timestamps.clear();
        newTimestamps.forEach(this::addTimestamp);
        notifySizeChanged();
        currentIndex = Math.min(currentIndex, timestamps.size() - 1);
        notifySelected();
    }

    public void addTimestampLive(long timestamp) {
        addTimestamp(timestamp);
        notifySizeChanged();
        if (liveMode) {
            currentIndex = timestamps.size() - 1;
            notifySelected();
        }
    }

    public void addObserver(ControllerObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ControllerObserver observer) {
        observers.remove(observer);
    }

    public void setCurrentIndex(int index) {
        liveMode = false;
        currentIndex = clamp(index, 0, timestamps.size() - 1);
        notifySelected();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isLive() {
        return liveMode;
    }

    public int getMaxIndex() {
        return Math.max(0, timestamps.size() - 1);
    }

    public int getSpeed() {
        return speed;
    }

    public int getMinSpeed() {
        return speedMin;
    }

    public int getMaxSpeed() {
        return speedMax;
    }

    public void playForward() {
        liveMode = false;
        playState = PlayState.PLAY_FORWARD;
        restartTimer();
    }

    public void playBackward() {
        liveMode = false;
        playState = PlayState.PLAY_BACKWARD;
        restartTimer();
    }

    public void pause() {
        playState = PlayState.PAUSED;
        playbackTimer.stop();
    }

    public void stepForward() {
        liveMode = false;
        playState = PlayState.PAUSED;
        step(+1);
    }

    public void stepBackward() {
        liveMode = false;
        playState = PlayState.PAUSED;
        step(-1);
    }

    public void jumpToStart() {
        liveMode = false;
        playState = PlayState.PAUSED;
        currentIndex = 0;
        notifySelected();
    }

    public void jumpToEnd() {
        liveMode = false;
        playState = PlayState.PAUSED;
        currentIndex = timestamps.size() - 1;
        notifySelected();
    }

    public void setSpeed(int step) {
        speed = clamp(step, speedMin, speedMax);
        restartTimer();
        notifySpeedChanged();
    }

    public void resetToLive() {
        liveMode = true;
        pause();
        currentIndex = timestamps.size() - 1;
        notifySelected();
    }

    public void playPause() {
        if (playState == PlayState.PAUSED) {
            playForward();
        } else {
            pause();
        }
    }

    public void speedUp() {
        setSpeed(speed + 1);
    }

    public void slowDown() {
        setSpeed(speed - 1);
    }

    /* ---------------- Internal ---------------- */

    private void addTimestamp(long timestamp) {
        int idx = Collections.binarySearch(timestamps, timestamp);
        if (idx >= 0) {
            return; // Already exists
        }
        int insertPos = -idx - 1;
        timestamps.add(insertPos, timestamp);
    }

    private void step() {
        if (playState == PlayState.PAUSED) return;
        step(playState == PlayState.PLAY_FORWARD ? +1 : -1);
    }

    private void step(int delta) {
        int next = currentIndex + delta;
        if (next < 0 || next >= timestamps.size()) {
            pause();
            return;
        }
        currentIndex = next;
        notifySelected();
    }

    private void notifySelected() {
        if (timestamps.isEmpty()) return;
        long ts = timestamps.get(currentIndex);
        for (ControllerObserver o : observers) {
            o.select(ts, currentIndex);
        }
    }

    private void notifySizeChanged() {
        for (ControllerObserver o : observers) {
            o.sizeChanged(timestamps.size());
        }
    }

    private void notifySpeedChanged() {
        for (ControllerObserver o : observers) {
            o.speedChanged(speed);
        }
    }

    private int computeDelayMs() {
        double multiplier = Math.pow(1.5, speed);
        double baseDelay = 100.0; // 1x speed
        return (int) Math.max(20, baseDelay / multiplier);
    }

    private void restartTimer() {
        playbackTimer.stop();
        if (playState != PlayState.PAUSED) {
            playbackTimer.setDelay(computeDelayMs());
            playbackTimer.start();
        }
    }

    private int clamp(int index, int min, int max) {
        return Math.max(min, Math.min(index, max));
    }
}
