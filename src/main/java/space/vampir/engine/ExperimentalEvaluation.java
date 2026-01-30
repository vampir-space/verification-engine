package space.vampir.engine;

import space.vampir.engine.message.Odometry;

import java.util.ArrayList;
import java.util.HashMap;


public class ExperimentalEvaluation {

    private final ArrayList<Observer> observers = new ArrayList<>();
    // Data storage
    private final HashMap<Long, Odometry> reference = new HashMap<>();
    private final HashMap<Long, Odometry> GNSS = new HashMap<>();
    private final HashMap<Long, Odometry> verificationEngine = new HashMap<>();

    // The agreement matrix
    int[][] matrix;

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
        matrix = getTableContent();
    }

    public int[][] getMatrix(){
        return matrix;
    }

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
        matrix = getTableContent();
        observers.forEach(Observer::update);
    }

    public void endEvaluation(){
        observers.forEach(Observer::finish);
    }

    /**
     * Calculates the confusion matrix based on the current 'diff' threshold.
     */
    private int[][] getTableContent() {
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

    }
}