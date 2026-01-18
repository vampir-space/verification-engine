package space.vampir.engine;

import org.junit.jupiter.api.Test;
import space.vampir.engine.message.Odometry;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EvaluationTest {
    @Test
    void calculateAgreementTableContentUniformTest() {
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

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation(0L, 3L, 1L);
        experimentalEvaluation.addOdometries(referenceExample, sensorAnAIExample, verificationEngineExample);

        var tableContent = experimentalEvaluation.getAgreementTableContent();
        assertEquals(1, tableContent[1][1]);
        assertEquals(1, tableContent[2][1]);
        assertEquals(1, tableContent[1][2]);
        assertEquals(1, tableContent[2][2]);
    }

    @Test
    void calculateConfusionTableContentTest() {
        ArrayList<Odometry> referenceExample = new ArrayList<>();
        ArrayList<Odometry> sensorAnAIExample = new ArrayList<>();
        ArrayList<Odometry> verificationEngineExample = new ArrayList<>();

        referenceExample.add(new Odometry(0, 0, 0, 0));
        referenceExample.add(new Odometry(1, 0, 0, 0));

        sensorAnAIExample.add(new Odometry(0, 0.4, 0.4, 0)); //tp
        sensorAnAIExample.add(new Odometry(1, 1, 1, 0)); //fn
        sensorAnAIExample.add(new Odometry(2, 0.4, 0.4, 0)); //fp

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation(0L, 3L, 1L);
        experimentalEvaluation.addOdometries(referenceExample, sensorAnAIExample, verificationEngineExample);


        HashMap<Long, Odometry> map = new HashMap<>();
        map.put(0L, sensorAnAIExample.get(0));
        map.put(1L, sensorAnAIExample.get(1));
        map.put(2L, sensorAnAIExample.get(2));

        var tableContent = experimentalEvaluation.getConfusionTableContent(map);
        assertEquals(1, tableContent[1][1]);
        assertEquals(1, tableContent[2][1]);
        assertEquals(1, tableContent[1][2]);
        assertEquals(1, tableContent[2][2]);
    }

    @Test
    void calculateAgreementTableContentIrregularTest() {
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

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation(0L, 3L, 1L);
        experimentalEvaluation.addOdometries(referenceExample, sensorAnAIExample, verificationEngineExample);

        var tableContent = experimentalEvaluation.getAgreementTableContent();
        assertEquals(1, tableContent[1][1]);
        assertEquals(2, tableContent[2][1]);
        assertEquals(0, tableContent[1][2]);
        assertEquals(1, tableContent[2][2]);
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

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation(0L, 3L, 1L);
        experimentalEvaluation.addOdometries(referenceExample, sensorAnAIExample, verificationEngineExample);


        experimentalEvaluation.startApplication();

        Thread.sleep(10000);
    }
}
