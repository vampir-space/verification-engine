package space.vampir.engine.visualization;

import space.vampir.engine.verification.UpdatedVerificationCase;
import space.vampir.engine.visualization.controller.KeyBindingManager;

import java.awt.*;

public abstract class Visualization {

    protected boolean enabled;

    protected final Dimension defaultDimension;

    public Visualization(boolean enabled, Dimension defaultDimension) {
        this.enabled = enabled;
        this.defaultDimension = defaultDimension;
    }

    protected abstract void doVisualize(UpdatedVerificationCase verificationCase);

    public void visualize(UpdatedVerificationCase verificationCase) {
        if (enabled) {
            doVisualize(verificationCase);
        }
    }

    protected abstract void startVisualization(Dimension dimension);

    public final void startWindow(Dimension dimension) {
        if (enabled) {
            startVisualization(dimension);
        }
    }

    public final void startWindow() {
        startWindow(defaultDimension);
    }

    protected abstract void updateVisualization();

    public final void updateWindow() {
        if (enabled) {
            updateVisualization();
        }
    }

    public void registerHotkeys(KeyBindingManager keyBindingManager) {
    }

}
