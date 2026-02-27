package space.vampir.engine.verification;

import org.junit.jupiter.api.Test;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.PointPillars;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.visualization.MapPanel;
import space.vampir.engine.visualization.MapRender;
import space.vampir.engine.visualization.RenderExample;
import space.vampir.engine.visualization.SceneVisualization;
import tools.refinery.mapconverter.map.MapHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VerificationEngineSystemTests {
    final MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_6_vis.svg"),
            293.64313 - 145.75468, 1143.4985 - 145.75468, 522.96765 - 165.92186,
            -100, 100, -40,
            47.478824, 19.056313);
    final File mapFile = new File(this.getClass().getResource("/Crosswalk_6.xodr").getFile());

    public void saveImage(Scenario scenario, String name)  {
        VerificationEngine verificationEngine = null;
        try {
            verificationEngine = new VerificationEngineWithRefinery(new MapHandler());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var updatedScenario = verificationEngine.update(scenario);

        SceneVisualization visualization = new SceneVisualization(map);
        visualization.show(updatedScenario);
        MapPanel mapPanel = new MapPanel(map);
        mapPanel.saveImage(new File("ve_" + name + ".png"), 10000, 4000);
    }

    @Test
    void simpleTest() {
        var scenario = new Scenario(
                new Odometry(0,47.47900448915325,19.056188607914573,Math.PI*1.5),
                new PointPillars(0, java.util.List.of(
                        new PointPillars.PointPillarsDetection(-3.9, 1.2, Math.PI*0,5,3)
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign",Math.PI*0.06,0.9)
                )));
        saveImage(scenario, "simple");
    }

    @Test
    void doubleDetectionTest() {
        var scenario = new Scenario(
                new Odometry(0,47.47903748915325,19.056240607914573,Math.PI*1.5),
                new PointPillars(0, java.util.List.of(
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign",Math.PI*0.03,0.5),
                        new Yolo.YoloDetection("sign",Math.PI*0.14,0.3)
                )));
        saveImage(scenario, "doubleDetectionTest");
    }

    @Test
    void yoloDetectionTest() {
        var scenario = new Scenario(
                new Odometry(0,47.47861548915325,19.057146607914573,Math.PI*1.6),
                new PointPillars(0, java.util.List.of(
                        new PointPillars.PointPillarsDetection(-11.2, 5.78, Math.PI*1.9,5,3)
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign",Math.PI*0.06,0.5),
                        new Yolo.YoloDetection("sign",Math.PI*0.136,0.3)
                )));
        saveImage(scenario, "yoloDetectionTest");
    }

    @Test
    void odometryDetectionTest() {
        var scenario = new Scenario(
                new Odometry(0,47.47904448915325,19.056378607914573,Math.PI*1.5),
                new PointPillars(0, java.util.List.of(
                )),
                new Yolo(0, List.of(
                )));
        saveImage(scenario, "odometryDetectionTest");
    }
}
