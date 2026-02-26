package space.vampir.engine;

import space.vampir.engine.message.Odometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;


public class ExperimentalEvaluation {

    private final ArrayList<Observer> observers = new ArrayList<>();
    // Data storage
    private final HashMap<Long, Odometry> reference = new HashMap<>();
    private final HashMap<Long, Odometry> GNSS = new HashMap<>();
    private final HashMap<Long, Odometry> verificationEngine = new HashMap<>();

    // The agreement matrix

    // The threshold variable controlled by the slider
    double diff = 0.5;


    //todo make it clearer
    long startTime = -1;
    long endTime = -1;

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void attach(Observer observer) {
        this.observers.add(observer);
    }

    public double getDiff() {
        return diff;
    }

    public void setDiff(double diff) {
        this.diff = diff;
    }

    public HashMap<Long, Odometry> getGNSS() {
        return GNSS;
    }

    public HashMap<Long, Odometry> getVerificationEngine() {
        return verificationEngine;
    }

    public void addOdometries(Map<Long, Odometry> ref, Map<Long, Odometry> gnss, Map<Long, Odometry> ver) {
        putAll(reference, ref);
        putAll(GNSS, gnss);
        putAll(verificationEngine, ver);
        observers.forEach(Observer::update);
        if (startTime == -1) {
            startTime = Collections.min(ref.keySet());
        }
        if (endTime == -1 || endTime < Collections.max(ref.keySet())) {
            endTime = Collections.max(ref.keySet());
        }
    }

    private void putAll(Map<Long, Odometry> target, Map<Long, Odometry> source) {
        for (Map.Entry<Long, Odometry> entry : source.entrySet()) {
            if (entry.getValue() != null) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void endEvaluation() {
        observers.forEach(Observer::finish);
    }

    /**
     * Calculates the confusion matrix based on the current 'diff' threshold.
     */
    public int[][] getMatrix() {
        return getMatrix(timeStamp -> true);
    }

    /**
     * Calculates the confusion matrix based on the current 'diff' threshold in the given period.
     */
    public int[][] getMatrix(long actualTime, long timeWindow) {
        return getMatrix(timeStamp -> timeStamp >= actualTime - timeWindow && timeStamp <= actualTime);
    }

    private int[][] getMatrix(Predicate<Long> timeFilter) {
        int tt = 0, ft = 0, tf = 0, ff = 0, to = 0, fo = 0;
        for (Long timeStamp : reference.keySet()) {
            if (timeFilter.test(timeStamp)) {
                if ((GNSS.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) && (GNSS.get(timeStamp).getY() - reference.get(timeStamp).getY() < diff)) {
                    if (verificationEngine.containsKey(timeStamp)) {
                        if (verificationEngine.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) {
                            tt++;
                        } else {
                            tf++;
                        }
                    } else {
                        to++;
                    }
                } else {
                    if (verificationEngine.containsKey(timeStamp)) {
                        if (verificationEngine.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) {
                            ft++;
                        } else {
                            ff++;
                        }
                    } else {
                        fo++;
                    }
                }
            }
        }

        return new int[][]{
                {tt, tf, to},
                {ft, ff, fo}
        };
    }

    /**
     * Helper method to calculate the percentage distribution of errors.
     */
    public double[] getDistribution(HashMap<Long, Odometry> dataList, int BIN_COUNT, double MAX_ERROR_RANGE, long actualTime, long timeWindow) {
        double[] bins = new double[BIN_COUNT];
        // If either map is empty, skip calculation
        if (dataList.isEmpty() || reference.isEmpty()) return bins;

        double step = MAX_ERROR_RANGE / BIN_COUNT;

        // Iterate through the keys of the provided map
        for (Long timeStamp : dataList.keySet()) {
            if (timeStamp >= actualTime - timeWindow && timeStamp <= actualTime) {
                // Check if this timestamp exists in the reference data
                if (reference.containsKey(timeStamp)) {
                    double error = Math.hypot(
                            dataList.get(timeStamp).getX() - reference.get(timeStamp).getX(),
                            dataList.get(timeStamp).getY() - reference.get(timeStamp).getY()
                    );

                    int binIndex = (int) (error / step);
                    if (binIndex >= BIN_COUNT) binIndex = BIN_COUNT - 1;
                    if (binIndex < 0) binIndex = 0;
                    bins[binIndex]++;
                }
            }
        }

        // Normalize to percentage (0.0 - 1.0)
        for (int i = 0; i < BIN_COUNT; i++) {
            bins[i] /= dataList.size();
        }
        return bins;
    }
}