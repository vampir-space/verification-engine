package space.vampir.engine.visualization.controller;

import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.visualization.Visualization;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;

public class ControlPanel extends Visualization {

    private final JFrame frame = new JFrame("Controller");
    private final JPanel panel = new JPanel();
    private final JSlider timeSlider;
    private final JSlider speedSlider;

    private boolean actionInitiated = false;

    public ControlPanel(Controller controller) {
        super(true, new Dimension(600, 120));
        panel.setLayout(new BorderLayout());

        /* -------- Buttons -------- */

        JLabel timeLabel = new JLabel("0 / 0");
        timeLabel.setPreferredSize(new Dimension(80, timeLabel.getPreferredSize().height));
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton stepBack = new JButton("⏮");   // ⏮
        JButton playBack = new JButton("◀");   // ◀
        JButton pause = new JButton("⏸");   // ⏸
        JButton playFwd = new JButton("▶");   // ▶
        JButton stepFwd = new JButton("⏭");   // ⏭
        JButton live = new JButton("(●)");

        stepBack.setToolTipText("Step Backward");
        playBack.setToolTipText("Play Backward");
        pause.setToolTipText("Pause");
        playFwd.setToolTipText("Play Forward");
        stepFwd.setToolTipText("Step Forward");
        live.setToolTipText("Go to Live");

        stepBack.addActionListener(al(e -> controller.stepBackward()));
        playBack.addActionListener(al(e -> controller.playBackward()));
        pause.addActionListener(al(e -> controller.pause()));
        playFwd.addActionListener(al(e -> controller.playForward()));
        stepFwd.addActionListener(al(e -> controller.stepForward()));
        live.addActionListener(al(e -> controller.resetToLive()));

        for (JButton button : new JButton[]{stepBack, playBack, pause, playFwd, stepFwd, live}) {
            InputMap im = button.getInputMap(JComponent.WHEN_FOCUSED);
            im.put(KeyStroke.getKeyStroke("SPACE"), "none");
        }

        JPanel bottomControls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomControls.add(timeLabel);
        bottomControls.add(stepBack);
        bottomControls.add(playBack);
        bottomControls.add(pause);
        bottomControls.add(playFwd);
        bottomControls.add(stepFwd);
        bottomControls.add(live);

        /* -------- Speed Slider -------- */

        speedSlider = new JSlider(controller.getMinSpeed(), controller.getMaxSpeed(), controller.getSpeed());
        speedSlider.setPreferredSize(new Dimension(100, 40));
        speedSlider.setMajorTickSpacing(1);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(cl(e -> controller.setSpeed(speedSlider.getValue())));
        InputMap imss = speedSlider.getInputMap(JComponent.WHEN_FOCUSED);
        imss.put(KeyStroke.getKeyStroke("LEFT"), "none");
        imss.put(KeyStroke.getKeyStroke("RIGHT"), "none");

        bottomControls.add(new JLabel("Speed"));
        bottomControls.add(speedSlider);

        /* -------- Time Slider -------- */

        timeSlider = new JSlider(0, 0, 0);
        timeSlider.addChangeListener(cl(e -> {
            if (!timeSlider.getValueIsAdjusting()) {
                controller.setCurrentIndex(timeSlider.getValue());
            }
        }));
        InputMap imts = timeSlider.getInputMap(JComponent.WHEN_FOCUSED);
        imts.put(KeyStroke.getKeyStroke("UP"), "none");
        imts.put(KeyStroke.getKeyStroke("DOWN"), "none");

        panel.add(timeSlider, BorderLayout.NORTH);
        panel.add(bottomControls, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        /* -------- Sync slider when controller updates -------- */

        controller.addObserver(new ControllerObserver() {
            @Override
            public void select(long ts, int idx) {
                update();
            }

            @Override
            public void sizeChanged(long maxTime, int size) {
                update();
            }

            @Override
            public void speedChanged(int speed) {
                update();
            }

            private void update() {
                SwingUtilities.invokeLater(() -> {
                    int current = controller.getCurrentIndex();
                    int max = controller.getMaxIndex();
                    timeLabel.setText(String.format("%d / %d", current, max));
                    live.setForeground(controller.isLive() ? Color.RED : Color.BLACK);
                    if (!actionInitiated) {
                        actionInitiated = true;
                        timeSlider.setMaximum(controller.getMaxIndex());
                        timeSlider.setValue(controller.getCurrentIndex());
                        speedSlider.setValue(controller.getSpeed());
                        actionInitiated = false;
                    }
                });
            }
        });
    }

    @Override
    public void startVisualization(Dimension dimension) {
        SwingUtilities.invokeLater(() -> {
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(dimension);
            frame.setContentPane(panel);
            frame.pack();
            frame.setLocation(0, 0);
            frame.setVisible(true);
        });
    }

    @Override
    public void updateVisualization() {
    }

    @Override
    public void doVisualize(UpdatedScenario updatedScenario) {
    }

    @Override
    public void registerHotkeys(KeyBindingManager keyBindingManager) {
        keyBindingManager.registerDefaultHotkeys(panel);
    }

    private ChangeListener cl(ChangeListener listener) {
        return e -> {
            if (!actionInitiated) {
                actionInitiated = true;
                listener.stateChanged(e);
                actionInitiated = false;
            }
        };
    }

    private ActionListener al(ActionListener listener) {
        return e -> {
            if (!actionInitiated) {
                actionInitiated = true;
                listener.actionPerformed(e);
                actionInitiated = false;
            }
        };
    }
}
