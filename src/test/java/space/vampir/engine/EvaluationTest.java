package space.vampir.engine;

import org.junit.jupiter.api.Test;
import space.vampir.engine.message.Odometry;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluationTest {
    @Test
    void calculateTableContentUniformTest() {
        HashMap<Long, Odometry> referenceExample = new HashMap<>();
        HashMap<Long, Odometry> GNSS = new HashMap<>();
        HashMap<Long, Odometry> verificationEngineExample = new HashMap<>();

        referenceExample.put(0L, new Odometry(0L, 0, 0, 0));
        referenceExample.put(1L, new Odometry(1L, 0, 0, 0));
        referenceExample.put(2L, new Odometry(2L, 0, 0, 0));
        referenceExample.put(3L, new Odometry(3L, 0, 0, 0));
        referenceExample.put(4L, new Odometry(4L, 0, 0, 0));
        referenceExample.put(5L, new Odometry(5L, 0, 0, 0));

        GNSS.put(0L, new Odometry(0, 0.4, 0.4, 0)); //T
        GNSS.put(1L, new Odometry(1, 1, 1, 0)); //F
        GNSS.put(2L, new Odometry(2, 0.4, 0.4, 0)); //T
        GNSS.put(3L, new Odometry(3, 2, 2, 0)); //F
        GNSS.put(4L, new Odometry(4, 0.4, 0.4, 0)); //T
        GNSS.put(5L, new Odometry(5, 2, 2, 0)); //F


        verificationEngineExample.put(0L, new Odometry(0, 0.3, 0.3, 0)); //T
        verificationEngineExample.put(1L, new Odometry(1, 0.4, 0.4, 0)); //T
        verificationEngineExample.put(2L, new Odometry(2, 1, 1, 0)); //F
        verificationEngineExample.put(3L, new Odometry(3, 1, 1, 0)); //F

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        experimentalEvaluation.addOdometries(referenceExample, GNSS, verificationEngineExample);

        var tableContent = experimentalEvaluation.getMatrix();
        assertEquals(1, tableContent[0][0]);
        assertEquals(1, tableContent[1][0]);
        assertEquals(1, tableContent[0][1]);
        assertEquals(1, tableContent[1][1]);
        assertEquals(1, tableContent[1][2]);
        assertEquals(1, tableContent[1][2]);
    }

    @Test
    void calculateTableContentIrregularTest() {
        HashMap<Long, Odometry> referenceExample = new HashMap<>();
        HashMap<Long, Odometry> sensorAnAIExample = new HashMap<>();
        HashMap<Long, Odometry> verificationEngineExample = new HashMap<>();


        referenceExample.put(0L, new Odometry(0L, 0, 0, 0));
        referenceExample.put(1L, new Odometry(1L, 0, 0, 0));
        referenceExample.put(2L, new Odometry(2L, 0, 0, 0));
        referenceExample.put(3L, new Odometry(3L, 0, 0, 0));

        sensorAnAIExample.put(0L, new Odometry(0, 0.4, 0.4, 0)); //T
        sensorAnAIExample.put(1L, new Odometry(1, 1, 1, 0)); //F
        sensorAnAIExample.put(2L, new Odometry(2, 0.6, 0.6, 0)); //F
        sensorAnAIExample.put(3L, new Odometry(3, 2, 2, 0)); //F

        verificationEngineExample.put(0L, new Odometry(0, 0.3, 0.3, 0)); //T
        verificationEngineExample.put(1L, new Odometry(1, 0.4, 0.4, 0)); //T
        verificationEngineExample.put(2L, new Odometry(2, 1, 1, 0)); //F
        verificationEngineExample.put(3L, new Odometry(3, 0.4, 0.4, 0)); //T

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        experimentalEvaluation.addOdometries(referenceExample, sensorAnAIExample, verificationEngineExample);

        var tableContent = experimentalEvaluation.getMatrix();
        assertEquals(1, tableContent[0][0]);
        assertEquals(2, tableContent[1][0]);
        assertEquals(0, tableContent[0][1]);
        assertEquals(1, tableContent[1][1]);
    }

    @Test
    void displayDataTest() throws InterruptedException {
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

        Thread.sleep(100000);
    }

    @Test
    void saveToCSVTest() throws InterruptedException {
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
        experimentalEvaluation.attach(new SaveToCSV(experimentalEvaluation));
        experimentalEvaluation.endEvaluation();
    }
}
