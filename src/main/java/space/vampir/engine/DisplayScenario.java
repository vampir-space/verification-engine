package space.vampir.engine;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.PointPillars;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.visualization.*;

import java.io.File;
import java.util.List;

public class DisplayScenario {
    final MapRender map;

    DisplayScenario(MapRender map) {
        this.map = map;
    }

    public static void main(String[] args) {
        // Map
        final MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_6_vis.svg"),
                293.64313 - 145.75468, 1143.4985 - 145.75468, 522.96765 - 165.92186,
                -100, 100, -40,
                47.478824, 19.056313);

        new DisplayScenario(map).showOnPanel(
                new Scenario(
                        new Odometry(0,47.47869148915325,19.057058607914573,Math.PI*1.5),
                        new PointPillars(0,List.of(
                                new PointPillars.PointPillarsDetection(0, 10, Math.PI*0,2,2),
                                new PointPillars.PointPillarsDetection(10, 10, Math.PI*0,1,1),
                                new PointPillars.PointPillarsDetection(-10, -10, Math.PI*0,5,5)
                        )),
                        new Yolo(0, List.of(
                                new Yolo.YoloDetection("car",Math.PI*0.04,0),
                                new Yolo.YoloDetection("car",Math.PI*0.08,0)))
                ));
    }

    public void showOnPanel(Scenario scenario) {
        SceneVisualization visualization = new SceneVisualization(map);
        visualization.show(scenario);
        visualization.getMapPanel().saveImage(new File("scene.png"), 1000, 400);
        visualization.updateWindow();
        visualization.startWindow();
    }
}
