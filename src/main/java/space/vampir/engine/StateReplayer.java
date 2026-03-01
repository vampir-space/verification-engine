package space.vampir.engine;

import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.UpdatedScenario;
import space.vampir.engine.verification.UpdatedVerificationCase;
import space.vampir.engine.verification.VerificationCase;
import space.vampir.engine.verification.VerificationEngine;
import space.vampir.engine.visualization.Visualization;
import space.vampir.engine.visualization.controller.ControlPanel;
import space.vampir.engine.visualization.controller.Controller;
import space.vampir.engine.visualization.controller.ControllerObserver;
import space.vampir.engine.visualization.controller.KeyBindingManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StateReplayer {

    private final VerificationEngine verificationEngine;
    private final Controller controller;
    private final List<Visualization> visualizations = new ArrayList<>();
    private final Map<Long, UpdatedVerificationCase> states = new LinkedHashMap<>();

    public StateReplayer(VerificationEngine verificationEngine) {
        this.verificationEngine = verificationEngine;

        controller = new Controller();
        ControlPanel controlPanel = new ControlPanel(controller);
        controller.addObserver((time, size) -> {
            UpdatedVerificationCase verificationCase = states.get(time);
            if (verificationCase != null) {
                for (Visualization visualization : visualizations) {
                    visualization.visualize(verificationCase);
                    visualization.updateWindow();
                }
            }
        });
        visualizations.add(controlPanel);
    }

    public void addVisualization(Visualization visualization) {
        visualizations.add(visualization);
    }

    public void addControllerObserver(ControllerObserver observer) {
        controller.addObserver(observer);
    }

    private void add(UpdatedVerificationCase state) {
        long time = state.scenario().time();
        states.put(time, state);
        controller.addTimestampLive(time);
    }

    public void addState(Scenario scenario) {
        UpdatedScenario updatedScenario = verificationEngine.update(scenario);
        UpdatedVerificationCase updatedVerificationCase = new UpdatedVerificationCase(updatedScenario, null);
        add(updatedVerificationCase);
    }

    public void addState(VerificationCase verificationCase) {
        Scenario scenario = verificationCase.scenario();
        UpdatedScenario updatedScenario = verificationEngine.update(scenario);
        UpdatedVerificationCase updatedVerificationCase = verificationCase.update(updatedScenario);
        add(updatedVerificationCase);
    }

    public void start() {
        visualizations.forEach(Visualization::startWindow);

        KeyBindingManager keyBindingManager = new KeyBindingManager(controller);
        for (Visualization visualization : visualizations) {
            visualization.registerHotkeys(keyBindingManager);
        }
    }
}
