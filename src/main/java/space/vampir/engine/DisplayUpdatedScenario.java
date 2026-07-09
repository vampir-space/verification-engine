package space.vampir.engine;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.PointPillars;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.UpdatedVerificationCase;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.SceneVisualization;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.List;

public class DisplayUpdatedScenario {
    final MapRender map;

    DisplayUpdatedScenario(MapRender map) {
        this.map = map;
    }

    public static void main(String[] args) {
        // Map
        final MapRender mapRender = new MapRender("/CrossWalk_6/CrossWalk_6.json");

        new DisplayUpdatedScenario(mapRender).showOnPanel(
                new UpdatedVerificationCase(
                        new UpdatedScenario(
                                new Scenario(
                                        new Odometry(0, 47.47869148915325, 19.0572, Math.PI * 1.5),
                                        new PointPillars(0, List.of(
                                                new PointPillars.PointPillarsDetection(5, 10, Math.PI * 0, 2, 2),
                                                new PointPillars.PointPillarsDetection(10, 10, Math.PI * 0, 1, 1),
                                                new PointPillars.PointPillarsDetection(-10, -10, Math.PI * 0, 5, 5)
                                        )),
                                        new Yolo(0, List.of(
                                                new Yolo.YoloDetection("car", Math.PI * 0.05, 0),
                                                new Yolo.YoloDetection("car", Math.PI * 0.08, 0)))
                                ),
                                new Odometry(0, 47.47859, 19.057058607914573, Math.PI * 1.5),
                                5
                        ),
                        new Odometry(0, 47.47869148915325, 19.057058607914573, Math.PI * 1.5)
                )
        );


        ///////////////////

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
        GNSS.put(4L, new Odometry(4L, 0.4, 0.4, 0)); //T
        GNSS.put(5L, new Odometry(5L, 2, 2, 0)); //F
        GNSS.put(6L, new Odometry(6L, 0.4, 0.4, 0)); //T

        verificationEngineExample.put(0L, new Odometry(0L, 0.3, 0.3, 0)); //T
        verificationEngineExample.put(1L, new Odometry(1L, 0.4, 0.4, 0)); //T
        verificationEngineExample.put(2L, new Odometry(2L, 1, 1, 0)); //F
        verificationEngineExample.put(3L, new Odometry(3L, 0.4, 0.4, 0)); //T
        verificationEngineExample.put(4L, new Odometry(4L, 1, 1, 0)); //F

        ExperimentalEvaluation experimentalEvaluation = new ExperimentalEvaluation(mapRender);
        experimentalEvaluation.addOdometries(referenceExample, GNSS, verificationEngineExample);

        //todo this way?
        experimentalEvaluation.attach(new VisualStatRepresentation(experimentalEvaluation));
        /// /
    }

    public void showOnPanel(UpdatedVerificationCase updatedVerificationCase) {
        SceneVisualization visualization = new SceneVisualization(map);
        visualization.show(updatedVerificationCase);
        visualization.getMapPanel().saveImage(new File("scene.png"), 1000, 400);
        visualization.updateWindow();
        visualization.startWindow(new Dimension(1200, 800));
    }
}
