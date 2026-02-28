package space.vampir.engine.visualization;

public class CliConfig {
    public boolean showScene = true;
    public boolean showStats = true;
    // add more views later, e.g.:
    // boolean showCamera = false;
    // boolean showRefinery = false;
    public String map = null;

    public static CliConfig get(String[] args) {
        CliConfig config = new CliConfig();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--scene" -> config.showScene = true;
                case "--no-scene" -> config.showScene = false;
                case "--stats" -> config.showStats = true;
                case "--no-stats" -> config.showStats = false;
                case "--map" -> {
                    if (i + 1 < args.length) {
                        config.map = args[i + 1];
                        i++;
                    } else {
                        System.err.println("Expected map name after --map");
                    }
                }
            }
        }
        return config;
    }
}
