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
    final MapRender map = new MapRender("/CrossWalk_6/CrossWalk_6.json");
    final File mapFile = new File(this.getClass().getResource("/Crosswalk_6.xodr").getFile());

    public void saveImage(Scenario scenario, String name) throws IOException {
        VerificationEngine verificationEngine = new VerificationEngineWithRefinery(new MapHandler());
        var updatedScenario = verificationEngine.update(scenario);

        SceneVisualization visualization = new SceneVisualization(map);
        visualization.show(updatedScenario);
        MapPanel mapPanel = new MapPanel(map);
        mapPanel.saveImage(new File("new_" + name + ".png"), 10000, 4000);
    }

    @Test
    void simpleTest() throws IOException {
        var scenario = new Scenario(
                new Odometry(0,47.47900448915325,19.056188607914573,Math.PI*1.5),
                new PointPillars(0, java.util.List.of(
                        new PointPillars.PointPillarsDetection(-3.9, 1.2, Math.PI*0,5,3)
                )),
                new Yolo(0, List.of(
                        new Yolo.YoloDetection("sign",Math.PI*0.06,0.9)
                )));
        saveImage(scenario, "test-XX");
    }
}
