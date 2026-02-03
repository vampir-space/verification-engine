package space.vampir.engine;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.PointPillars;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.visualization.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class DisplayUpdatedScenario {
    final MapRender map;

    DisplayUpdatedScenario(MapRender map) {
        this.map = map;
    }

    public static void main(String[] args) {
        // Map
        final MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_6_vis.svg"),
                293.64313 - 145.75468, 1143.4985 - 145.75468, 522.96765 - 165.92186,
                -100, 100, -40,
                47.478824, 19.056313);

        new DisplayUpdatedScenario(map).showOnPanel(
                new UpdatedScenario(
                        new Scenario(
                            new Odometry(0,47.47869148915325,19.0572,Math.PI*1.5),
                            new PointPillars(0, List.of(
                                    new PointPillars.PointPillarsDetection(5, 10, Math.PI*0,2,2),
                                    new PointPillars.PointPillarsDetection(10, 10, Math.PI*0,1,1),
                                    new PointPillars.PointPillarsDetection(-10, -10, Math.PI*0,5,5)
                            )),
                            new Yolo(0, List.of(
                                    new Yolo.YoloDetection("car",Math.PI*0.05,0),
                                    new Yolo.YoloDetection("car",Math.PI*0.08,0)))
                        ),
                        new Odometry(0,47.47859,19.057058607914573,Math.PI*1.5),
                        new Odometry(0,47.47869148915325,19.057058607914573,Math.PI*1.5)
                ));
    }

    public void showOnPanel(UpdatedScenario updatedScenario) {
        JFrame frame = new JFrame();

        UpdatedSceneVisualization visualization = new UpdatedSceneVisualization(map);
        visualization.show(updatedScenario);
        MapPanel mapPanel = new MapPanel(map);

        mapPanel.saveImage(new File("scene.png"), 1000, 400);
        SwingUtilities.updateComponentTreeUI(frame);

        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(1200, 800));
            frame.setContentPane(mapPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
