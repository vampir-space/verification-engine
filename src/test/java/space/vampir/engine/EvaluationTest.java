package space.vampir.engine;

import org.junit.jupiter.api.Test;
import space.vampir.engine.message.Odometry;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluationTest {
    @Test
    void calculateTableContentUniformTest() {
        ArrayList<Odometry> referenceExample = new ArrayList<>();
        ArrayList<Odometry> sensorAnAIExample = new ArrayList<>();
        ArrayList<Odometry> verificationEngineExample = new ArrayList<>();

        referenceExample.add(new Odometry(0, 0, 0, 0));
        referenceExample.add(new Odometry(1, 0, 0, 0));
        referenceExample.add(new Odometry(2, 0, 0, 0));
        referenceExample.add(new Odometry(3, 0, 0, 0));

        sensorAnAIExample.add(new Odometry(0, 0.4, 0.4, 0)); //T
        sensorAnAIExample.add(new Odometry(1, 1, 1, 0)); //F
        sensorAnAIExample.add(new Odometry(2, 0.4, 0.4, 0)); //T
        sensorAnAIExample.add(new Odometry(3, 2, 2, 0)); //F

        verificationEngineExample.add(new Odometry(0, 0.3, 0.3, 0)); //T
        verificationEngineExample.add(new Odometry(1, 0.4, 0.4, 0)); //T
        verificationEngineExample.add(new Odometry(2, 1, 1, 0)); //F
        verificationEngineExample.add(new Odometry(3, 1, 1, 0)); //F

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        experimentalEvaluation.addOdometries(referenceExample.get(0), sensorAnAIExample.get(0), verificationEngineExample.get(0));
        experimentalEvaluation.addOdometries(referenceExample.get(1), sensorAnAIExample.get(1), verificationEngineExample.get(1));
        experimentalEvaluation.addOdometries(referenceExample.get(2), sensorAnAIExample.get(2), verificationEngineExample.get(2));
        experimentalEvaluation.addOdometries(referenceExample.get(3), sensorAnAIExample.get(3), verificationEngineExample.get(3));

        var tableContent = experimentalEvaluation.getTableContent();
        assertEquals(1, tableContent[0][0]);
        assertEquals(1, tableContent[1][0]);
        assertEquals(1, tableContent[0][1]);
        assertEquals(1, tableContent[1][1]);
    }

    @Test
    void calculateTableContentIrregularTest() {
        ArrayList<Odometry> referenceExample = new ArrayList<>();
        ArrayList<Odometry> sensorAnAIExample = new ArrayList<>();
        ArrayList<Odometry> verificationEngineExample = new ArrayList<>();

        referenceExample.add(new Odometry(0, 0, 0, 0));
        referenceExample.add(new Odometry(1, 0, 0, 0));
        referenceExample.add(new Odometry(2, 0, 0, 0));
        referenceExample.add(new Odometry(3, 0, 0, 0));

        sensorAnAIExample.add(new Odometry(0, 0.4, 0.4, 0)); //T
        sensorAnAIExample.add(new Odometry(1, 1, 1, 0)); //F
        sensorAnAIExample.add(new Odometry(2, 0.6, 0.6, 0)); //F
        sensorAnAIExample.add(new Odometry(3, 2, 2, 0)); //F

        verificationEngineExample.add(new Odometry(0, 0.3, 0.3, 0)); //T
        verificationEngineExample.add(new Odometry(1, 0.4, 0.4, 0)); //T
        verificationEngineExample.add(new Odometry(2, 1, 1, 0)); //F
        verificationEngineExample.add(new Odometry(3, 0.4, 0.4, 0)); //T

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        experimentalEvaluation.addOdometries(referenceExample.get(0), sensorAnAIExample.get(0), verificationEngineExample.get(0));
        experimentalEvaluation.addOdometries(referenceExample.get(1), sensorAnAIExample.get(1), verificationEngineExample.get(1));
        experimentalEvaluation.addOdometries(referenceExample.get(2), sensorAnAIExample.get(2), verificationEngineExample.get(2));
        experimentalEvaluation.addOdometries(referenceExample.get(3), sensorAnAIExample.get(3), verificationEngineExample.get(3));

        var tableContent = experimentalEvaluation.getTableContent();
        assertEquals(1, tableContent[0][0]);
        assertEquals(2, tableContent[1][0]);
        assertEquals(0, tableContent[0][1]);
        assertEquals(1, tableContent[1][1]);
    }

    @Test
    void displayDataTest() throws InterruptedException {
        ArrayList<Odometry> referenceExample = new ArrayList<>();
        ArrayList<Odometry> sensorAnAIExample = new ArrayList<>();
        ArrayList<Odometry> verificationEngineExample = new ArrayList<>();

        referenceExample.add(new Odometry(0, 0, 0, 0));
        referenceExample.add(new Odometry(1, 0, 0, 0));
        referenceExample.add(new Odometry(2, 0, 0, 0));
        referenceExample.add(new Odometry(3, 0, 0, 0));

        sensorAnAIExample.add(new Odometry(0, 0.4, 0.4, 0)); //T
        sensorAnAIExample.add(new Odometry(1, 1, 1, 0)); //F
        sensorAnAIExample.add(new Odometry(2, 0.6, 0.6, 0)); //F
        sensorAnAIExample.add(new Odometry(3, 2, 2, 0)); //F

        verificationEngineExample.add(new Odometry(0, 0.3, 0.3, 0)); //T
        verificationEngineExample.add(new Odometry(1, 0.4, 0.4, 0)); //T
        verificationEngineExample.add(new Odometry(2, 1, 1, 0)); //F
        verificationEngineExample.add(new Odometry(3, 0.4, 0.4, 0)); //T

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        experimentalEvaluation.addOdometries(referenceExample.get(0), sensorAnAIExample.get(0), verificationEngineExample.get(0));
        experimentalEvaluation.addOdometries(referenceExample.get(1), sensorAnAIExample.get(1), verificationEngineExample.get(1));
        experimentalEvaluation.addOdometries(referenceExample.get(2), sensorAnAIExample.get(2), verificationEngineExample.get(2));
        experimentalEvaluation.addOdometries(referenceExample.get(3), sensorAnAIExample.get(3), verificationEngineExample.get(3));

        experimentalEvaluation.startApplication();

        Thread.sleep(10000);
    }
}
