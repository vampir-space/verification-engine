package space.vampir.engine;

import org.junit.jupiter.api.Test;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.PointPillars;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.verification.DummyVerificationEngine;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.visualization.MapPanel;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.RenderExample;
import space.vampir.engine.visualization.SceneVisualization;

import java.io.File;
import java.util.List;

public class SystemTest {
    // Map
    final MapRender map = new MapRender("/CrossWalk_6/CrossWalk_6.json");

    @Test
    void simpleTest() {
        var scenario = new Scenario(
                new Odometry(0, 47.47900448915325, 19.056188607914573, Math.PI * 1.5),
                new PointPillars(0, java.util.List.of(
                        new PointPillars.PointPillarsDetection(-3.9, 1.2, Math.PI * 0, 5, 3)
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign", Math.PI * 0.06, 0.9)
                )));
        saveImage(scenario, "test-simpleTest");
    }

    @Test
    void doubleDetectionTest() {
        var scenario = getDoubleDetectionScenario();
        saveImage(scenario, "test-doubleDetectionTest");
    }

    private Scenario getDoubleDetectionScenario() {
        return new Scenario(
                new Odometry(0, 47.47903748915325, 19.056240607914573, Math.PI * 1.5),
                new PointPillars(0, java.util.List.of(
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign", Math.PI * 0.03, 0.5),
                        new Yolo.YoloDetection("sign", Math.PI * 0.14, 0.3)
                )));
    }

    @Test
    void yoloDetectionTest() {
        var scenario = new Scenario(
                new Odometry(0, 47.47861548915325, 19.057146607914573, Math.PI * 1.6),
                new PointPillars(0, java.util.List.of(
                        new PointPillars.PointPillarsDetection(-11.2, 5.78, Math.PI * 1.9, 5, 3)
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign", Math.PI * 0.06, 0.5),
                        new Yolo.YoloDetection("sign", Math.PI * 0.136, 0.3)
                )));
        map.saveImage(scenario, "test-yoloDetectionTest");
    }

    @Test
    void odometryDetectionTest() {
        var scenario = new Scenario(
                new Odometry(0, 47.47904448915325, 19.056378607914573, Math.PI * 1.5),
                new PointPillars(0, java.util.List.of(
                )),
                new Yolo(0, List.of(
                )));
        saveImage(scenario, "test-odometryDetectionTest");
    }

    @Test
    void statePlayerTestWithDummyGNSSAndDummyVerificationEngine() throws InterruptedException {
        final int NUMBER_OF_GENERATED_SCENARIOS = 100;

        VerificationEngine verificationEngine = new DummyVerificationEngine(0.8);

        StateReplayer stateReplayer = new StateReplayer(verificationEngine);

        SceneVisualization sceneVisualization = new SceneVisualization(map);
        stateReplayer.addVisualization(sceneVisualization);

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation();
        VisualStatRepresentation statsVisualization = new VisualStatRepresentation(experimentalEvaluation, true, false);
        stateReplayer.addControllerObserver(statsVisualization);
        stateReplayer.addVisualization(statsVisualization);

        Scenario initialScenario = getDoubleDetectionScenario();

        for (int i = 0; i < NUMBER_OF_GENERATED_SCENARIOS; i++) {
            UpdatedScenario updatedScenario = new UpdatedScenario(
                    new Scenario(
                            i,
                            NoiseApplier.addNoise(initialScenario.odometry(), 1.1),
                            initialScenario.pointPillars(),
                            initialScenario.yolo()
                    ),
                    null,
                    initialScenario.odometry()
            );
            stateReplayer.addState(updatedScenario);
        }

        stateReplayer.start();
        Thread.sleep(100000);
    }
}
