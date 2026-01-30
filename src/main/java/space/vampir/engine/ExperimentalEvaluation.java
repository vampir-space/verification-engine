package space.vampir.engine;

import space.vampir.engine.message.Odometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class ExperimentalEvaluation {

    private final ArrayList<Observer> observers = new ArrayList<>();
    // Data storage
    private final HashMap<Long, Odometry> reference = new HashMap<>();
    private final HashMap<Long, Odometry> GNSS = new HashMap<>();
    private final HashMap<Long, Odometry> verificationEngine = new HashMap<>();

    // The agreement matrix
    //int[][] matrix;

    // The threshold variable controlled by the slider
    double diff = 0.5;


    //todo make it clearer
    long startTime = -1;
    long endTime = -1;

    public long getStartTime(){
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
      //  matrix = getTableContent();
    }

    /*public int[][] getMatrix(){
        return matrix;
    }*/

    public HashMap<Long, Odometry> getGNSS() {
        return GNSS;
    }

    public HashMap<Long, Odometry> getVerificationEngine() {
        return verificationEngine;
    }

    public void addOdometries(HashMap<Long, Odometry> ref, HashMap<Long, Odometry> gnss, HashMap<Long,Odometry> ver){
        reference.putAll(ref);
        GNSS.putAll(gnss);
        verificationEngine.putAll(ver);
        //matrix = getTableContent();
        observers.forEach(Observer::update);
        if(startTime == -1) startTime = Collections.min(ref.keySet());
        if(endTime == -1 || endTime < Collections.max(ref.keySet())) endTime = Collections.max(ref.keySet());
    }

    public void endEvaluation(){
        observers.forEach(Observer::finish);
    }

    /**
     * Calculates the confusion matrix based on the current 'diff' threshold.
     */
    public int[][] getMatrix() {
        int tt = 0, ft = 0, tf = 0, ff  = 0, to = 0, fo = 0;
        for (Long timeStamp : reference.keySet()) {
            if((GNSS.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) && (GNSS.get(timeStamp).getY() - reference.get(timeStamp).getY() < diff)){
                if(verificationEngine.containsKey(timeStamp)){
                    if(verificationEngine.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) tt++;
                    else tf++;
                }
                else to++;

            }
            else{
                if(verificationEngine.containsKey(timeStamp)){
                    if(verificationEngine.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) ft++;
                    else ff++;
                }
                else fo++;
            }

        }
        return new int[][]{
                {tt, tf, to},
                {ft, ff, fo}
        };
    }

    /**
     * Calculates the confusion matrix based on the current 'diff' threshold.
     */
    public int[][] getMatrix(long actualTime, long timeWindow) {
        int tt = 0, ft = 0, tf = 0, ff  = 0, to = 0, fo = 0;
        for (Long timeStamp : reference.keySet()) {
            if(timeStamp >= actualTime - timeWindow && timeStamp <= actualTime) {
                if((GNSS.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) && (GNSS.get(timeStamp).getY() - reference.get(timeStamp).getY() < diff)){
                    if(verificationEngine.containsKey(timeStamp)){
                        if(verificationEngine.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) tt++;
                        else tf++;
                    }
                    else to++;

                }
                else{
                    if(verificationEngine.containsKey(timeStamp)){
                        if(verificationEngine.get(timeStamp).getX() - reference.get(timeStamp).getX() < diff) ft++;
                        else ff++;
                    }
                    else fo++;
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
    public double[] getDistribution(HashMap<Long, Odometry> dataList, int BIN_COUNT, double MAX_ERROR_RANGE) {
        double[] bins = new double[BIN_COUNT];
        // Ha bármelyik map üres, ne is kezdjünk számolni
        if (dataList.isEmpty() || reference.isEmpty()) return bins;

        double step = MAX_ERROR_RANGE / BIN_COUNT;

        // Iteráljunk végig a kapott map kulcsain
        for (Long timeStamp : dataList.keySet()) {
            // Ellenőrizzük, hogy a referencia adatokban is megvan-e ez az időbélyeg
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

        // Normalizálás százalékra (0.0 - 1.0)
        for (int i = 0; i < BIN_COUNT; i++) {
            bins[i] /= dataList.size();
        }
        return bins;
    }

    /**
     * Main method for testing purposes.
     */
    public static void main(String[] args) {
        HashMap<Long, Odometry> referenceExample = new HashMap<>();
        HashMap<Long, Odometry> GNSS = new HashMap<>();
        HashMap<Long, Odometry> verificationEngineExample = new HashMap<>();

        referenceExample.put(0L, new Odometry(0L, 0, 0, 0));
        referenceExample.put(1L, new Odometry(1L, 0, 0, 0));
        referenceExample.put(2L, new Odometry(2L, 0, 0, 0));
        referenceExample.put(3L, new Odometry(3L, 0, 0, 0));
        referenceExample.put(4L, new Odometry(4L, 0, 0, 0));
        referenceExample.put(5L, new Odometry(5L, 0, 0, 0));
        referenceExample.put(6L, new Odometry(6L, 0, 0, 0));

        GNSS.put(0L, new Odometry(0L, 0.4, 0.4, 0)); //T
        GNSS.put(1L, new Odometry(1L, 1, 1, 0)); //F
        GNSS.put(2L, new Odometry(2L, 0.6, 0.6, 0)); //F
        GNSS.put(3L, new Odometry(3L, 2, 2, 0)); //F
        GNSS.put(4L,new Odometry(4L, 0.4, 0.4, 0)); //T
        GNSS.put(5L, new Odometry(5L, 2, 2, 0)); //F
        GNSS.put(6L,new Odometry(6L, 0.4, 0.4, 0)); //T

        verificationEngineExample.put(0L, new Odometry(0L, 0.3, 0.3, 0)); //T
        verificationEngineExample.put(1L, new Odometry(1L, 0.4, 0.4, 0)); //T
        verificationEngineExample.put(2L, new Odometry(2L, 1, 1, 0)); //F
        verificationEngineExample.put(3L, new Odometry(3L, 0.4, 0.4, 0)); //T
        verificationEngineExample.put(4L, new Odometry(4L, 1, 1, 0)); //F

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        experimentalEvaluation.addOdometries(referenceExample, GNSS, verificationEngineExample);

        //todo this way?
        experimentalEvaluation.attach(new VisualRepresentation(experimentalEvaluation));
    }
}