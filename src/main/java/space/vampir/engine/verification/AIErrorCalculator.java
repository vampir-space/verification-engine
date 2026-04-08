package space.vampir.engine.verification;

import space.vampir.engine.geometry.GeometrySolver;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.visualization.MapRender;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.mapconverter.map.Converter;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.map.MapObject;
import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.Circle;
import tools.refinery.mapconverter.scope.Point;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.*;
import tools.refinery.store.reasoning.seed.ModelSeed;

import java.io.IOException;
import java.util.*;

public class AIErrorCalculator implements VerificationEngine{
    final Converter<ModelSeedFragment> converter;
    ModelSeedStrategy outputStrategy;
    ComplexityStrategy<ModelSeedFragment> complexityStrategy;
    MapGenerationProblemProvider problemProvider;

    // TODO: remove this dependency, reorganize
    final MapRender mapRender;

    final VerificationEngineConfiguration configuration;

    final int targetID;

    public AIErrorCalculator(MapHandler map, MapRender mapRender, int targetID) throws IOException {
        this(map, mapRender, targetID, new VerificationEngineConfiguration());

    }

    public AIErrorCalculator(MapHandler map, MapRender mapRender, int targetID, VerificationEngineConfiguration configuration) throws IOException {
        this.outputStrategy = new ModelSeedStrategy();

        complexityStrategy = new BasicWithTypeRefinementStrategy<>(outputStrategy);
        converter = new Converter<>(map, complexityStrategy);

        problemProvider = new MapGenerationProblemProvider(complexityStrategy.getMetaModelString());

        this.mapRender = mapRender;

        this.configuration = configuration;

        this.targetID = targetID;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        if(rawScenario.yolo().getYoloDetections().size() != 1) {
            System.out.println(Math.PI);
            return new UpdatedScenario(rawScenario,
                    new Odometry(
                            rawScenario.time(),
                            rawScenario.odometry().getX(),
                            rawScenario.odometry().getY(),
                            rawScenario.odometry().getTheta()));
        }
        if (rawScenario.odometry() == null){
            System.out.println("No odometry");
            return new UpdatedScenario(rawScenario,
                    new Odometry(
                            rawScenario.time(),
                            rawScenario.odometry().getX(),
                            rawScenario.odometry().getY(),
                            rawScenario.odometry().getTheta()));
        }
        System.out.println("X");
        // EGO
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
        // cclockwise
        var theta = Math.PI / 2 - rawScenario.odometry().getTheta();
        Point egoPosition = new Point(xyCoords[0], xyCoords[1]);

        // MAP
        Map<String, Yolo.YoloDetection> observationYoloMap = new HashMap<>();
        Circle relevantMap = new Circle(egoPosition, configuration.relevantMapSegmentSize);
        Scope<ModelSeedFragment> scope = converter.getScope(relevantMap);
        var objects = scope.getAllElements().getObjects();
        var targetPosition = objects.get(targetID).getCoordinate();

        // Detection
        var yoloDetection = rawScenario.yolo().getYoloDetections().get(0);
        // clockwise
        var observationAngle = yoloDetection.angle();

        System.out.println(observationAngle);
//        Math.atan2()

        return new UpdatedScenario(rawScenario,
                new Odometry(
                        rawScenario.time(),
                        rawScenario.odometry().getX(),
                        rawScenario.odometry().getY(),
                        rawScenario.odometry().getTheta()));
    }

    private Scope<ModelSeedFragment> translateToScope(Scenario rawScenario, Map<String, Yolo.YoloDetection> observationYoloMap) {
        // todo
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
        var theta = Math.PI / 2 - rawScenario.odometry().getTheta();
        Point egoPosition = new Point(xyCoords[0], xyCoords[1]);

        Circle relevantMap = new Circle(egoPosition, configuration.relevantMapSegmentSize);
        Scope<ModelSeedFragment> scope = converter.getScope(relevantMap);
        if (configuration.doRoadCutting) {
            scope.roadCutter(configuration.roadCutterGranularity);
        }

//        complexityStrategy.setEgoRange(configuration.gnssConfidenceRange);
//        MapObject car = new MapObject(1, ObjectType.Car, egoPosition, new Size(1.0, 1.0));
//        scope.addEgo(car);

        var objectSelection = new ObjectSelection(scope);

        System.out.println("YoloDetections:");
        final List<Yolo.YoloDetection> yoloDetections = rawScenario.yolo() == null ? List.of() : rawScenario.yolo().getYoloDetections();
        for (int i = 0; i < yoloDetections.size(); i++) {
            final var yoloDetection = yoloDetections.get(i);


//            final Sector sector = new Sector(
//                    egoPosition,
//                    configuration.yoloRange,
//                    theta-yoloDetection.angle(),
//                    0.1,
//                    1,
//                    0.1);
//            final ArrayList<ObjectType> types = new ArrayList<>();
//            types.add(ObjectType.Signal);
//            LinkedHashMap<Integer, MapObject> objects2  = scope.getMapObjects(sector, types);
//
            LinkedHashMap<Integer, MapObject> objects2 = objectSelection.getObjects(
                    egoPosition.getX(),
                    egoPosition.getY(),
                    theta - yoloDetection.angle(),
                    configuration.gnssConfidenceRange,
                    configuration.yoloAngleOfView,
                    configuration.yoloRange,
                    ObjectType.Signal);

            var observation = scope.addObjectObservations(objects2, "yolo_"+i, ObjectType.Signal);
            observationYoloMap.put(observation.getId(),yoloDetection);
            System.out.println(observation.getId()+":");
            for(var possible : objects2.values()) {
                System.out.println(possible.getId());
            }
            System.out.println("end");


        }
        return scope;
    }
}
