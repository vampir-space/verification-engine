package space.vampir.engine.verification;

import org.jetbrains.annotations.NotNull;
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
import tools.refinery.mapconverter.scope.Sector;
import tools.refinery.mapconverter.scope.Size;
import tools.refinery.mapconverter.transform.BasicWithTypeRefinementStrategy;
import tools.refinery.mapconverter.transform.ComplexityStrategy;
import tools.refinery.mapconverter.transform.MapGenerationProblemProvider;
import tools.refinery.mapconverter.transform.ModelSeedFragment;
import tools.refinery.mapconverter.transform.ModelSeedStrategy;
import tools.refinery.store.reasoning.seed.ModelSeed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VerificationEngineWithRefinery implements VerificationEngine {
    final Converter<ModelSeedFragment> converter;

    ModelSeedStrategy outputStrategy;
    BasicWithTypeRefinementStrategy<ModelSeedFragment> complexityStrategy;

    MapGenerationProblemProvider problemProvider;

    // TODO: remove this dependency, reorganize
    final MapRender mapRender;

    final VerificationEngineConfiguration configuration;

    // cache
    // Scope for map
    Scope<ModelSeedFragment> mapOnlyScope;
    // Fragment for map
    ModelSeedStrategy mapOnlyFragment;

    public VerificationEngineWithRefinery(MapHandler map, MapRender mapRender) throws IOException {
        this(map, mapRender, new VerificationEngineConfiguration(), null);
    }

    public VerificationEngineWithRefinery(MapHandler map, MapRender mapRender, VerificationEngineConfiguration configuration) throws IOException {
        this(map, mapRender, configuration, null);
    }

    public VerificationEngineWithRefinery(MapHandler map, MapRender mapRender, String metamodelPath) throws IOException {
        this(map, mapRender, new VerificationEngineConfiguration(), metamodelPath);
    }

    public VerificationEngineWithRefinery(MapHandler map, MapRender mapRender, VerificationEngineConfiguration configuration, String metamodelPath) throws IOException {
        this.outputStrategy = new ModelSeedStrategy();

        complexityStrategy = metamodelPath == null || metamodelPath.isBlank()
                ? new BasicWithTypeRefinementStrategy<>(outputStrategy)
                : new BasicWithTypeRefinementStrategy<>(outputStrategy, metamodelPath);
        converter = new Converter<>(map, complexityStrategy);

        problemProvider = new MapGenerationProblemProvider(complexityStrategy.getMetaModelString());

        this.mapRender = mapRender;

        this.configuration = configuration;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        if (mapOnlyScope == null) {
            var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
            Point egoPosition = new Point(xyCoords[0], xyCoords[1]);
            this.mapOnlyScope = translateMapToScope(egoPosition);
            mapOnlyScope.translateMap1();
            this.mapOnlyFragment = outputStrategy;
        }

        if (rawScenario.yolo().getYoloDetections().isEmpty()) {
            return noUpdate(rawScenario);
        }

        List<String> inc = new ArrayList<>();
        Map<String, Yolo.YoloDetection> observationYoloMap = new HashMap<>();

        // reset scope
        Scope<ModelSeedFragment> updatedScope = new Scope<>(this.mapOnlyScope);
        // reset fragment
        outputStrategy = new ModelSeedStrategy(mapOnlyFragment);
        complexityStrategy.setOutputStrategy(outputStrategy);

        Scope<ModelSeedFragment> scope = translateToScope(rawScenario, updatedScope, observationYoloMap, inc);

        // stage 2
        // instead of
        // ModelSeedFragment refineryFragment = scope.translateMap();
        ModelSeedFragment refineryFragment = scope.translateMap2();
        ModelSeed modelSeed = refineryFragment.buildSeed();

        ModelGenerator model = outputStrategy.problemProvider.solve(modelSeed);
        if (!model.isLastGenerationSuccessful()) {
            return rejectUpdate(rawScenario);
        }

        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
        var theta = Math.PI / 2 - rawScenario.odometry().getTheta();
        GeometrySolver.OdometryPrior odometryPrior = new GeometrySolver.OdometryPrior(
                xyCoords[0],
                xyCoords[1],
                rawScenario.odometry().getUncertaintyInMeters(),
                theta);

        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(xyCoords[0], xyCoords[1], rawScenario.odometry().getUncertaintyInMeters()));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();

        if (model != null) {
            var pr = model.getProblemTrace().getPartialRelation("object");

            var observationMappingCursor = model.getPartialInterpretation(pr).getAll();
            while (observationMappingCursor.move()) {
                int observationIndex = observationMappingCursor.getKey().get(0);
                var observationName = refineryFragment.getName(observationIndex);
                var observation = observationYoloMap.get(observationName);

                int landMarkIndex = observationMappingCursor.getKey().get(1);
                var landmark = refineryFragment.getName(landMarkIndex);
                var landmarkCoordinate = scope.getCoordinate(landmark);


                yolos.add(new GeometrySolver.YoloDetection(landmarkCoordinate.getX(), landmarkCoordinate.getY(), -observation.angle(), configuration.yoloAngleOfView));
            }
//            model.serialize()
            GeometrySolver.Solution geometrySolution = GeometrySolver.solve(odometryPrior, locations, yolos);
            var coordsInGeo = mapRender.toGeoCoord(geometrySolution.x, geometrySolution.y);

            return new UpdatedScenario(rawScenario, new Odometry(rawScenario.time(), coordsInGeo[0], coordsInGeo[1], rawScenario.odometry().getTheta()));
        } else {
            return rejectUpdate(rawScenario);
        }
    }

    private static @NotNull UpdatedScenario rejectUpdate(Scenario rawScenario) {
        return new UpdatedScenario(rawScenario, null);
    }

    private static @NotNull UpdatedScenario noUpdate(Scenario rawScenario) {
        return new UpdatedScenario(rawScenario,
                new Odometry(
                        rawScenario.time(),
                        rawScenario.odometry().getX(),
                        rawScenario.odometry().getY(),
                        rawScenario.odometry().getTheta()));
    }

    Scope<ModelSeedFragment> translateMapToScope(Point egoPosition) {
        Circle relevantMap = new Circle(egoPosition, configuration.relevantMapSegmentSize);
        Scope<ModelSeedFragment> scope = converter.getScope(relevantMap);
        if (configuration.doRoadCutting) {
            scope.roadCutter(configuration.roadCutterGranularity);
        }
        return scope;
    }

    private Scope<ModelSeedFragment> translateToScope(
            Scenario rawScenario,
            Scope<ModelSeedFragment> scope,
            Map<String, Yolo.YoloDetection> observationYoloMap,
            List<String> s) {
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
        var theta = Math.PI / 2 - rawScenario.odometry().getTheta();
        Point egoPosition = new Point(xyCoords[0], xyCoords[1]);

        var objectSelection = new ObjectSelection(scope);

        final List<Yolo.YoloDetection> yoloDetections = rawScenario.yolo() == null ? List.of() : rawScenario.yolo().getYoloDetections();
        for (int i = 0; i < yoloDetections.size(); i++) {
            final var yoloDetection = yoloDetections.get(i);
            if (yoloDetection.confidence() >= configuration.yoloMinConfidence) {

                var objects2 = objectSelection.getObjects(
                        egoPosition.getX(),
                        egoPosition.getY(),
                        theta - yoloDetection.angle(),
                        rawScenario.odometry().getUncertaintyInMeters(),
                        configuration.yoloAngleOfView,
                        configuration.yoloRange,
                        ObjectType.Signal);

                objects2.sort((x, y) -> Double.compare(x.angleDiff(), y.angleDiff()));
                var objects3 = new LinkedHashMap<Integer, MapObject>();
                if (!objects2.isEmpty()) {
                    var r = objects2.getFirst();
                    objects3.put(r.id(), r.mapObject());
                }

                var observation = scope.addObjectObservations(objects3, "yolo_" + i, ObjectType.Signal);
                observationYoloMap.put(observation.getId(), yoloDetection);

                if (observation.getObjects().keySet().isEmpty()) {
                    s.add(observation.getId());
                }
            }
        }
        return scope;
    }
}
