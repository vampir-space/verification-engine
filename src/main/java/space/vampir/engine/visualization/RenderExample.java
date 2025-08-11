package space.vampir.engine.visualization;

import javax.swing.*;
import java.awt.*;

public class RenderExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MapRender map = new MapRender(RenderExample.class.getResource("/CrossWalk_3_objects.svg"),
                    275.242965-145.75,1164.145456-145.75,546.6958-125.81,
                    -60,60,-20);

            map.getObjects().add(
                    new ObjectRender(RenderExample.class.getResource("/car.svg"),
                        5,0,0,3));
            map.getObjects().add(
                    new ObjectRender(RenderExample.class.getResource("/blue-circle.svg"),
                            25,0,5,0));
            map.getObjects().add(
                    new ObjectRender(RenderExample.class.getResource("/orange-square.svg"),
                            2,1,3,0));

            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(400, 400));
            frame.setContentPane(new MapPanel(map));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}