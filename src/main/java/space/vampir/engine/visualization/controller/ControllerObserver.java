package space.vampir.engine.visualization.controller;

public interface ControllerObserver {
    void select(long time, int index);

    default void sizeChanged(int size) {
    }

    default void speedChanged(int speed) {
    }
}
