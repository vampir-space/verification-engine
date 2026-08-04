package space.vampir.engine;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.visualization.MapRender;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;


public class ExperimentalEvaluation {
    private final MapRender mapRender;

    private final ArrayList<Observer> observers = new ArrayList<>();
    // Data storage
    private final SortedUniqueList<Long> timestamps = new SortedUniqueList<>();
    private final HashMap<Long, Odometry> reference = new HashMap<>();
    private final HashMap<Long, Odometry> GNSS = new HashMap<>();
    private final HashMap<Long, UpdatedScenario> verificationEngine = new HashMap<>();


    // The threshold variable controlled by the slider
    double diff = 0.5;

    public ExperimentalEvaluation(MapRender mapRender) {
        if (mapRender == null) {
            throw new IllegalArgumentException("MapRender not defined");
        }
        this.mapRender = mapRender;
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

    public HashMap<Long, UpdatedScenario> getVerificationEngine() {
        return verificationEngine;
    }

    /**
     * Adds odometry data from multiple sources to the respective collections and notifies observers.
     * This method updates the reference, GNSS, and verification engine maps by adding entries
     * from the provided odometry data. It also updates the list of timestamps and notifies all
     * registered observers about the changes.
     *
     * @param ref  the map containing reference odometry data
     * @param gnss the map containing GNSS odometry data
     * @param ver  the map containing verification engine odometry data
     */
    public void addOdometries(Map<Long, Odometry> ref, Map<Long, Odometry> gnss, Map<Long, UpdatedScenario> ver) {
        putAll(reference, ref);
        putAll(GNSS, gnss);
        putAll(verificationEngine, ver);
        timestamps.addAll(reference.keySet());
        observers.forEach(Observer::update);
    }

    public void clearOdometries() {
        reference.clear();
        GNSS.clear();
        verificationEngine.clear();
        timestamps.clear();
        observers.forEach(Observer::update);
    }

    /**
     * Copies all entries from the source map into the target map.
     * Only entries with non-null values in the source map are added to the target map.
     *
     * @param target the map to which entries will be copied
     * @param source the map from which entries will be copied
     */
    private <T> void putAll(Map<Long, T> target, Map<Long, T> source) {
        for (Map.Entry<Long, T> entry : source.entrySet()) {
            if (entry.getValue() != null) {
                target.put(entry.getKey(), entry.getValue());
            } else {
                throw new IllegalArgumentException("Value is null: " + entry.getKey());
            }
        }
    }

    /**
     * Marks the end of the evaluation process by notifying all registered observers.
     * This method iterates through the list of observers and invokes their {@code finish()} method.
     */
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

    /**
     * Computes a 2x3 matrix based on the provided filtering criteria for timestamp indices
     * and corresponding timestamps. The matrix represents validation results under
     * specific conditions:
     * <p>
     * [ [tt, tf, to],
     * [ft, ff, fo] ]
     * <p>
     * where:
     * - tt: True positives for GNSS and verification engine.
     * - tf: GNSS true positives but verification engine false negatives.
     * - to: GNSS true positives but verification engine is off.
     * - ft: GNSS false negatives but verification engine true positives.
     * - ff: False negatives for both GNSS and verification engine.
     * - fo: GNSS false negatives but verification engine is off.
     *
     * @param timeFilter A {@code BiPredicate} that filters the indices and timestamps to be
     *                   included in the evaluation.
     * @return A 2x3 integer matrix containing counts calculated based on the given filter.
     */
    private int[][] getMatrix(BiPredicate<Integer, Long> timeFilter) {
        // Row 0: GNSS Valid
        int tt = 0, tf = 0, to = 0;
        // Row 1: GNSS Invalid
        int ft = 0, ff = 0, fo = 0;
        // Row 2: GNSS Off (New states)
        int ot = 0, of = 0, oo = 0;

        for (int i = 0; i < timestamps.size(); i++) {
            Long timeStamp = timestamps.get(i);

            if (timeFilter.test(i, timeStamp)) {

                // 1. Determine GNSS Status
                boolean isGnssOff = !GNSS.containsKey(timeStamp);
                boolean isGnssValid = false;

                if (!isGnssOff) {
                    var distance = getDistanceInM(GNSS.get(timeStamp), reference.get(timeStamp));
                    isGnssValid = distance < diff;
                }

                // 2. Determine Verification Engine (VE) Status
                boolean isVeOff = !verificationEngine.containsKey(timeStamp) || !verificationEngine.get(timeStamp).use();
                boolean isVeValid = false;

                if (!isVeOff) {
                    var distance = getDistanceInM(verificationEngine.get(timeStamp).updatedByVerificationEngine(), reference.get(timeStamp));
                    isVeValid = distance < diff;
                }

                // 3. Categorize into the 3x3 Matrix
                if (isGnssOff) {
                    if (isVeOff) oo++;
                    else if (isVeValid) ot++;
                    else of++;
                } else if (isGnssValid) {
                    if (isVeOff) to++;
                    else if (isVeValid) tt++;
                    else tf++;
                } else { // GNSS is Present but Invalid
                    if (isVeOff) fo++;
                    else if (isVeValid) ft++;
                    else ff++;
                }
            }
        }

        // Return the updated 3x3 matrix
        // Row 0: GNSS Valid   (VE Valid, VE Invalid, VE Off)
        // Row 1: GNSS Invalid (VE Valid, VE Invalid, VE Off)
        // Row 2: GNSS Off     (VE Valid, VE Invalid, VE Off)
        return new int[][]{
                {tt, tf, to},
                {ft, ff, fo},
                {ot, of, oo}
        };
    }

    protected double getDistanceInM(Odometry o1, Odometry o2) {
        if (o1 == null || o2 == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        var m1 = mapRender.toMapCoord(o1.getX(), o1.getY());
        var m2 = mapRender.toMapCoord(o2.getX(), o2.getY());
        var xdiff = m1[0] - m2[0];
        var ydiff = m1[1] - m2[1];
        return Math.sqrt(xdiff * xdiff + ydiff * ydiff);
    }
    protected double[] getRelError(Odometry reference, Odometry measurement) {
        var r = mapRender.toMapCoord(reference.getX(), reference.getY());
        var m = mapRender.toMapCoord(measurement.getX(), measurement.getY());
        var theta = reference.getTheta();

        var dx = m[0] - r[0];
        var dy = m[1] - r[1];

        var forward = dx * Math.sin(theta) + dy * Math.cos(theta);
        var sideways = dx * Math.cos(theta) - dy * Math.sin(theta);

        return new double[]{forward, sideways};
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

    void print() {
        print(System.out);
    }

    void print(PrintStream printStream) {
        printStream.print("time");
        printStream.print(",messageTimeInterval");
        printStream.print(",isInsideBorder");
        printStream.print(",detections");

        printStream.print(",gt-GNSS");
        printStream.print(",gt-GNSS-fwd");
        printStream.print(",gt-GNSS-rgt");
        printStream.print(",gt-VE");

        printStream.print(",GNSS-confidence");
        printStream.print(",VE-confidence");

        printStream.print(",VE-use-donotuse");
        printStream.println();

        for (long t : timestamps) {
            Odometry gt = reference.get(t);
            Odometry gnss = GNSS.get(t);
            UpdatedScenario ve = verificationEngine.get(t);

            var gtGNSS = getDistanceInM(gt, gnss);

            var use = ve.use();
            final double gtVE;
            final double cVE;
            if (use) {
                gtVE = getDistanceInM(gt, ve.updatedByVerificationEngine());
                cVE = ve.updatedByVerificationEngine().getUncertaintyInMeters();
            } else {
                gtVE = 0.0;
                cVE = 0.0;
            }
            var detections = ve.numberOfLandmarks();

            var relError = getRelError(gt,gnss);

            //printStream.println(t+","+referenceGNSS+","+referenceVE+","+referenceReliable+","+use+","+detections);

            printStream.print(t);
            printStream.print("," + ve.scenario().timeInterval());
            printStream.print("," + ve.scenario().isInsideMapBorder());
            printStream.print("," + detections);

            printStream.print("," + gtGNSS);
            printStream.print("," + relError[0]);
            printStream.print("," + relError[1]);
            printStream.print("," + gtVE);

            printStream.print("," + gnss.getUncertaintyInMeters());
            printStream.print("," + cVE);

            printStream.print("," + use);
            printStream.println();
        }
    }

}