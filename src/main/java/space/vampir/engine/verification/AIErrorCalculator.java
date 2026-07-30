package space.vampir.engine.verification;

import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.visualization.MapRender;
import tools.refinery.mapconverter.map.Converter;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.scope.Circle;
import tools.refinery.mapconverter.scope.Point;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.*;

import java.io.IOException;
import java.util.*;

public class AIErrorCalculator implements VerificationEngine{
    final Converter<ModelSeedFragment> converter;
    ModelSeedStrategy outputStrategy;
    ComplexityStrategy<ModelSeedFragment> complexityStrategy;

    // TODO: remove this dependency, reorganize
    final MapRender mapRender;

    final VerificationEngineConfiguration configuration;

    final int targetID;

    public AIErrorCalculator(MapHandler map, MapRender mapRender, int targetID) throws IOException {
        this(map, mapRender, targetID, new VerificationEngineConfiguration(), null);

    }

    public AIErrorCalculator(MapHandler map, MapRender mapRender, int targetID, VerificationEngineConfiguration configuration) throws IOException {
        this(map, mapRender, targetID, configuration, null);
    }

    public AIErrorCalculator(MapHandler map, MapRender mapRender, int targetID, String metamodelPath) throws IOException {
        this(map, mapRender, targetID, new VerificationEngineConfiguration(), metamodelPath);
    }

    public AIErrorCalculator(MapHandler map, MapRender mapRender, int targetID, VerificationEngineConfiguration configuration, String metamodelPath) throws IOException {
        this.outputStrategy = new ModelSeedStrategy();

        complexityStrategy = metamodelPath == null || metamodelPath.isBlank()
                ? new BasicWithTypeRefinementStrategy<>(outputStrategy)
                : new BasicWithTypeRefinementStrategy<>(outputStrategy, metamodelPath);
        converter = new Converter<>(map, complexityStrategy);

        this.mapRender = mapRender;

        this.configuration = configuration;

        this.targetID = targetID;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        if (rawScenario.odometry() == null){
            System.out.println("No odometry");
            return new UpdatedScenario(rawScenario,
                    new Odometry(
                            rawScenario.time(),
                            rawScenario.odometry().getX(),
                            rawScenario.odometry().getY(),
                            rawScenario.odometry().getTheta(),
                            rawScenario.odometry().getUncertaintyInMeters()),
                    0,
                    new ArrayList<>());
        }
        // EGO
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
        // cclockwise
        var theta = Math.PI / 2 - rawScenario.odometry().getTheta();
        Point egoPosition = new Point(xyCoords[0], xyCoords[1]);

        // MAP
        Circle relevantMap = new Circle(egoPosition, configuration.relevantMapSegmentSize);
        Scope<ModelSeedFragment> scope = converter.getScope(relevantMap);
        var objects = scope.getAllElements().getObjects();

        var targetPosition = objects.get(targetID).getCoordinate();


        // Filter for confidence > 0.4
        var yoloDetections = rawScenario.yolo() == null
                ? new ArrayList<Yolo.YoloDetection>()
                : rawScenario.yolo().getYoloDetections().stream()
                  .filter(d -> d.confidence() > 0.0)
                  .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        var lowestAngelError = Math.PI;
        var confidence = 0.0;
        // Detection
        for (var yoloDetection : yoloDetections) {
            // clockwise
            var observationAngle = yoloDetection.angle();
            var targetBearingGlobal = Math.atan2(targetPosition.getY() - egoPosition.getY(), targetPosition.getX() - egoPosition.getX());
            var actualRelativeAngle = -(targetBearingGlobal - theta);
            var rawError = observationAngle - actualRelativeAngle;
            // Wrap to [-PI, PI]
            var normalizedError = Math.atan2(Math.sin(rawError), Math.cos(rawError));
            var absoluteError = Math.abs(normalizedError);
            if (absoluteError < lowestAngelError) {
                lowestAngelError = absoluteError;
                confidence = yoloDetection.confidence();
            }

        }
        System.out.println(rawScenario.time() + "," + targetID + "," + egoPosition.distance(targetPosition) + "," + lowestAngelError + "," + confidence + "," + yoloDetections.size());

        return new UpdatedScenario(rawScenario,
                new Odometry(
                        rawScenario.time(),
                        rawScenario.odometry().getX(),
                        rawScenario.odometry().getY(),
                        rawScenario.odometry().getTheta(),
                        rawScenario.odometry().getUncertaintyInMeters()),
                0,
                new ArrayList<>());
    }
}
