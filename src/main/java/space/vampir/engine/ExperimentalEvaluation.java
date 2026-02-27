package space.vampir.engine;

import space.vampir.engine.message.Odometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;


public class ExperimentalEvaluation {

    private final ArrayList<Observer> observers = new ArrayList<>();
    // Data storage
    private final SortedUniqueList<Long> timestamps = new SortedUniqueList<>();
    private final HashMap<Long, Odometry> reference = new HashMap<>();
    private final HashMap<Long, Odometry> GNSS = new HashMap<>();
    private final HashMap<Long, Odometry> verificationEngine = new HashMap<>();

    // The agreement matrix

    // The threshold variable controlled by the slider
    double diff = 0.5;

    public void attach(Observer observer) {
        this.observers.add(observer);
    }

    public double getDiff() {
        return diff;
    }

    public void setDiff(double diff) {
        this.diff = diff;
    }

    public int getSize() {
        return reference.size();
    }

    public int getSizeBefore(long time) {
        int result = Collections.binarySearch(timestamps, time);
        return result < 0 ? -result - 1 : result + 1;
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
        timestamps.addAll(reference.keySet());
        observers.forEach(Observer::update);
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
        return getMatrix((index, timeStamp) -> true);
    }

    /**
     * Calculates the confusion matrix based on the current 'diff' threshold in the given period.
     */
    public int[][] getMatrix(int actualTime, int timeWindow) {
        return getMatrix((index, timeStamp) -> index > actualTime - timeWindow && index <= actualTime);
    }

    private int[][] getMatrix(BiPredicate<Integer, Long> timeFilter) {
        int tt = 0, ft = 0, tf = 0, ff = 0, to = 0, fo = 0;
        for (int i = 0; i < timestamps.size(); i++) {
            Long timeStamp = timestamps.get(i);
            if (timeFilter.test(i, timeStamp)) {
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
    public double[] getDistribution(HashMap<Long, Odometry> dataList, int BIN_COUNT, double MAX_ERROR_RANGE, int actualTime, int timeWindow) {
        double[] bins = new double[BIN_COUNT];
        // If either map is empty, skip calculation
        if (dataList.isEmpty() || reference.isEmpty()) return bins;

        double step = MAX_ERROR_RANGE / BIN_COUNT;
        int totalCount = 0;

        // Iterate through the keys of the provided map
        for (int i = 0; i < timestamps.size(); i++) {
            Long timeStamp = timestamps.get(i);
            if (i > actualTime - timeWindow && i <= actualTime) {
                // Check if this timestamp exists in reference and dataList as well
                if (reference.containsKey(timeStamp) && dataList.containsKey(timeStamp)) {
                    double error = Math.hypot(
                            dataList.get(timeStamp).getX() - reference.get(timeStamp).getX(),
                            dataList.get(timeStamp).getY() - reference.get(timeStamp).getY()
                    );

                    int binIndex = (int) (error / step);
                    if (binIndex >= BIN_COUNT) binIndex = BIN_COUNT - 1;
                    if (binIndex < 0) binIndex = 0;
                    bins[binIndex]++;
                    totalCount++;
                }
            }
        }

        // Normalize to percentage (0.0 - 1.0)
        for (int i = 0; i < BIN_COUNT; i++) {
            bins[i] /= totalCount;
        }
        return bins;
    }
}