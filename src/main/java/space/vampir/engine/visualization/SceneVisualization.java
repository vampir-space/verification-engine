package space.vampir.engine.visualization;

import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.UpdatedVerificationCase;
import space.vampir.engine.visualization.controller.KeyBindingManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;

public class SceneVisualization extends Visualization {
    protected static URL lineImage = SceneVisualization.class.getResource("/line.svg");
    protected static URL lineImage2 = SceneVisualization.class.getResource("/line2.svg");
    protected static URL carImage = SceneVisualization.class.getResource("/car.svg");
    protected static URL egoImage = SceneVisualization.class.getResource("/ego.svg");
    protected static URL laneImage = SceneVisualization.class.getResource("/lane.svg");
    protected static URL gnssImage = SceneVisualization.class.getResource("/gnss.svg");
    protected static URL veImage = SceneVisualization.class.getResource("/ve.svg");
    protected static URL gtImage = SceneVisualization.class.getResource("/gt.svg");
    protected static URL objectImage = SceneVisualization.class.getResource("/red-square.svg");

    protected final JFrame frame = new JFrame("Map");

    protected MapRender map;
    protected final MapPanel mapPanel;
    protected final JComboBox<String> mapSelector;
    protected final JLabel mapSelectorLabel = new JLabel();
    protected final ActionListener mapSelectorActionListener;

    final ObjectRender ego = new ObjectRender(egoImage, "ego", 3, 5, 0, 0, 0);
    final ObjectRender circle = new ObjectRender(RenderExample.class.getResource("/blue-circle.svg"), 30, 30, 0, 0, 0);
    final ObjectRender gnss = new ObjectRender(gnssImage, 3, 5, 0, 0, 0);
    final ObjectRender ve = new ObjectRender(veImage, 3, 5, 0, 0, 0);
    final ObjectRender gt = new ObjectRender(gtImage, "gt", 3, 5, 0, 0, 0);

    public SceneVisualization(MapRender map, boolean enabled) {
        super(enabled, new Dimension(700, 700));

        List<String> maps = MapProvider.getMapConfigs();
        this.map = map == null ? new MapRender(maps.getFirst()) : map;
        this.mapPanel = new MapPanel(this.map);
        setLabel();

        maps.add("Other");
        mapSelector = new JComboBox<>(maps.toArray(new String[0]));
        setMapSelector(this.map);
        mapSelectorActionListener = e -> {
            String selected = (String) mapSelector.getSelectedItem();
            if (selected != null && !selected.equals("Other")) {
                this.map = new MapRender(selected);
                mapPanel.setMapRender(this.map);
                setLabel();
            } else {
                setMapSelector(this.map);
            }
        };
        mapSelector.addActionListener(mapSelectorActionListener);
    }

    public SceneVisualization(MapRender map) {
        this(map, true);
    }

    private void setMapSelector(MapRender mapRender) {
        mapSelector.removeActionListener(mapSelectorActionListener);
        for (int i = 0; i < mapSelector.getItemCount(); i++) {
            if (mapSelector.getItemAt(i).equals(mapRender.mapConfig)) {
                mapSelector.setSelectedIndex(i);
                mapSelector.addActionListener(mapSelectorActionListener);
                return;
            }
        }
        mapSelector.setSelectedItem("Other");
        mapSelector.addActionListener(mapSelectorActionListener);
    }

    private void setLabel() {
        this.mapSelectorLabel.setText(String.format("%s", map.getName()));
    }

    @Override
    public void startVisualization(Dimension dimension) {
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(dimension);
            frame.setLayout(new BorderLayout());

            JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            topPanel.add(mapSelector);
            topPanel.add(mapSelectorLabel);
            JScrollPane scrollPane = new JScrollPane(
                    topPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );
            frame.add(scrollPane, BorderLayout.NORTH);
            frame.add(mapPanel, BorderLayout.CENTER);

            frame.pack();

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension frameSize = frame.getSize();
            frame.setLocation(screenSize.width - frameSize.width, 0);

            frame.setVisible(true);
        });
    }

    @Override
    public void updateVisualization() {
        SwingUtilities.updateComponentTreeUI(mapPanel);
    }

    @Override
    public void doVisualize(UpdatedVerificationCase verificationCase) {
        if (verificationCase.updatedScenario().updatedByVerificationEngine() == null) {
            show(verificationCase.scenario());
        } else {
            show(verificationCase);
        }
    }

    @Override
    public void registerHotkeys(KeyBindingManager keyBindingManager) {
        keyBindingManager.registerDefaultHotkeys(mapPanel);
    }

    public MapPanel getMapPanel() {
        return mapPanel;
    }

    public synchronized void show(Scenario state) {
        map.clearObjects();
        var odom = state.odometry();
        if (odom != null) {
            var coord = map.toMapCoord(odom.getX(), odom.getY());

            ego.setX(coord[0]);
            ego.setY(coord[1]);
            ego.setTheta(odom.getTheta());
            circle.setX(coord[0]);
            circle.setY(coord[1]);

            map.addObject(ego);
            map.addObject(circle);

            map.addObject(new ObjectRender(lineImage, circle.getSizeX(), circle.getSizeY(), ego.getX(), ego.getY(), ego.getTheta()));
        }

        var yolo = state.yolo();
        if (yolo != null) {
            for (var detection : yolo.getYoloDetections()) {
                final URL line;
                if (detection.type().equals("car")) {
                    line = lineImage;
                } else {
                    line = lineImage2;
                }

                var o = new ObjectRender(line, 12, 80, ego.getX(), ego.getY(), ego.getTheta() + detection.angle());
                map.addObject(o);
            }
        }

        var pointPillars = state.pointPillars();
        if (pointPillars != null) {
            for (var detection : pointPillars.getDetections()) {

                var cosT = Math.cos(ego.theta);
                var sinT = Math.sin(ego.theta);
                var o = new ObjectRender(
                        carImage,
                        detection.sizeY(),
                        detection.sizeX(),
                        ego.getX() - detection.posX() * cosT + detection.posY() * sinT,
                        ego.getY() + detection.posX() * sinT + detection.posY() * cosT,
                        detection.theta() + ego.getTheta());
                map.addObject(o);
            }
        }
    }

    public synchronized void show(UpdatedVerificationCase verificationCase) {
        map.clearObjects();

        Scenario state = verificationCase.scenario();



        //Drawing car based on gnss
        var gnssOdom = state.odometry();
        if (gnssOdom != null) {
            var coord = map.toMapCoord(gnssOdom.getX(), gnssOdom.getY());

            gnss.setX(coord[0]);
            gnss.setY(coord[1]);
            gnss.setTheta(gnssOdom.getTheta());
            circle.setX(coord[0]);
            circle.setY(coord[1]);

            circle.setSizeX(gnssOdom.getUncertaintyInMeters()*4);
            circle.setSizeY(gnssOdom.getUncertaintyInMeters()*4);

            map.addObject(circle);
            map.addObject(gnss);
        }

        //drawing car based on ground truth
        var gtOdom = verificationCase.groundTruth();
        if (gtOdom != null) {
            var coord = map.toMapCoord(gtOdom.getX(), gtOdom.getY());

            gt.setX(coord[0]);
            gt.setY(coord[1]);
            gt.setTheta(gtOdom.getTheta());
            map.addObject(gt);
        }

        //drawing car based on ve
        var veOdom = verificationCase.updatedByVerificationEngine();
        if (veOdom != null) {
            var coord = map.toMapCoord(veOdom.getX(), veOdom.getY());

            ve.setX(coord[0]);
            ve.setY(coord[1]);
            ve.setTheta(veOdom.getTheta());
            map.addObject(ve);
        }

        var yolo = state.yolo();
        if (yolo != null) {
            for (var detection : yolo.getYoloDetections()) {
                final URL line;
                if (detection.type().equals("car")) {
                    line = lineImage;
                } else {
                    line = lineImage2;
                }

                final ObjectRender source;
                if (gtOdom != null) {
                    source = gt;
                } else {
                    source = gnss;
                }
                var o = new ObjectRender(line, 12, 80, source.getX(), source.getY(), source.getTheta() + detection.angle());
                map.addObject(o);
            }
        }

        var pointPillars = state.pointPillars();
        if (pointPillars != null) {
            for (var detection : pointPillars.getDetections()) {

                var cosT = Math.cos(gnss.theta);
                var sinT = Math.sin(gnss.theta);
                var o = new ObjectRender(
                        carImage,
                        detection.sizeY(),
                        detection.sizeX(),
                        gnss.getX() - detection.posX() * cosT + detection.posY() * sinT,
                        gnss.getY() + detection.posX() * sinT + detection.posY() * cosT,
                        detection.theta() + gnss.getTheta());
                map.addObject(o);
            }
        }
    }
}
