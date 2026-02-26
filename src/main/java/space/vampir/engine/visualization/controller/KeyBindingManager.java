package space.vampir.engine.visualization.controller;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class KeyBindingManager {

    private final Controller controller;

    public KeyBindingManager(Controller controller) {
        this.controller = controller;
    }

    public void registerDefaultHotkeys(JPanel panel) {
        registerHotKey(
                "jumpToStart",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0),
                e -> controller.jumpToStart()
        );

        registerHotKey(
                "stepBackward",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                e -> controller.stepBackward()
        );

        registerHotKey(
                "playPause",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                e -> controller.playPause()
        );

        registerHotKey(
                "stepForward",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                e -> controller.stepForward()
        );

        registerHotKey(
                "jumpToEnd",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_END, 0),
                e -> controller.jumpToEnd()
        );

        registerHotKey(
                "live",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_L, 0),
                e -> controller.resetToLive()
        );

        registerHotKey(
                "slowDown",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0),
                e -> controller.slowDown()
        );

        registerHotKey(
                "resetSpeed",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_1, 0),
                e -> controller.setSpeed(0)
        );

        registerHotKey(
                "speedUp",
                panel,
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0),
                e -> controller.speedUp()
        );
    }

    public void registerHotKey(String name, JPanel panel, KeyStroke keyStroke, Consumer<ActionEvent> action) {
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);
        panel.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        });
    }
}
