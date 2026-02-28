package space.vampir.engine.visualization;

public class WindowConfig {
    public boolean showScene = true;
    public boolean showStats = true;
    // add more views later, e.g.:
    // boolean showCamera = false;
    // boolean showRefinery = false;

    public static WindowConfig get(String[] args) {
        WindowConfig config = new WindowConfig();
        for (String arg : args) {
            switch (arg) {
                case "--scene" -> config.showScene = true;
                case "--no-scene" -> config.showScene = false;
                case "--stats" -> config.showStats = true;
                case "--no-stats" -> config.showStats = false;
                // add more options later
            }
        }
        return config;
    }
}
