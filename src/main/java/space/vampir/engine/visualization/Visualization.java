package space.vampir.engine.visualization;

import java.awt.*;

public abstract class Visualization {

    protected boolean enabled;

    protected final Dimension defaultDimension;

    public Visualization(boolean enabled, Dimension defaultDimension) {
        this.enabled = enabled;
        this.defaultDimension = defaultDimension;
    }

    public abstract void startVisualization(Dimension dimension);

    public final void startWindow(Dimension dimension) {
        if (enabled) {
            startVisualization(dimension);
        }
    }

    public final void startWindow() {
        startWindow(defaultDimension);
    }

    public abstract void updateVisualization();

    public final void updateWindow() {
        if (enabled) {
            updateVisualization();
        }
    }

}
