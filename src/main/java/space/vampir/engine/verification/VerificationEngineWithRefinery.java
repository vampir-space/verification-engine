package space.vampir.engine.verification;

import org.jetbrains.annotations.NotNull;
import space.vampir.engine.geometry.GeometrySolver;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.mapconverter.map.Converter;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.map.MapObject;
import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.*;
import tools.refinery.mapconverter.transform.*;
import tools.refinery.store.reasoning.seed.ModelSeed;

import java.io.IOException;
import java.util.*;

public class VerificationEngineWithRefinery implements VerificationEngine {

    final Converter<ModelSeedFragment> converter;
    ModelSeedStrategy outputStrategy;
    MapGenerationProblemProvider problemProvider;

    public VerificationEngineWithRefinery(MapHandler map) throws IOException {
        outputStrategy = new ModelSeedStrategy();

        ComplexityStrategy<ModelSeedFragment> complexityStrategy = new BasicStrategy<>(outputStrategy);
        converter = new Converter<>(map, complexityStrategy);

        MapGenerationProblemProvider problemProvider =
                new MapGenerationProblemProvider(complexityStrategy.getMetaModelString());
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        Map<String,Yolo.YoloDetection> observationYoloMap = new HashMap<>();
        Scope<ModelSeedFragment> scope = translateToScope(rawScenario,observationYoloMap);

        ModelSeedFragment refineryFragment = scope.translateMap();
        ModelSeed modelSeed = refineryFragment.buildSeed();

        ModelGenerator model = outputStrategy.problemProvider.solve(modelSeed);

        GeometrySolver.OdometryPrior odometryPrior = new GeometrySolver.OdometryPrior(
                rawScenario.odometry().getX(),
                rawScenario.odometry().getY(),
                rawScenario.odometry().getTheta(),
                0.4);
        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(rawScenario.odometry().getX(), rawScenario.odometry().getY(), 10.0));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();

        if (model != null) {
            var observationMapping = problemProvider.getPartialRelation("Observation::Object");
            var observationMappingCursor = model.getPartialInterpretation(observationMapping).getAll();
            while (observationMappingCursor.move()) {
                int observationIndex = observationMappingCursor.getKey().get(0);
                var observationName = refineryFragment.getName(observationIndex);
                var observation = observationYoloMap.get(observationName);

                int landMarkIndex = observationMappingCursor.getKey().get(1);
                var landmark = refineryFragment.getName(landMarkIndex);
                var landmarkCoordinate = scope.getCoordinate(landmark);


                yolos.add(new GeometrySolver.YoloDetection(landmarkCoordinate.getX(), landmarkCoordinate.getY(), observation.angle(), Math.PI/12));
            }
        }
        GeometrySolver.Solution geometrySolution = GeometrySolver.solve(odometryPrior,locations,yolos);

        return new UpdatedScenario(rawScenario,new Odometry(rawScenario.time(),geometrySolution.x,geometrySolution.y,geometrySolution.alpha),null);
    }

    private @NotNull Scope<ModelSeedFragment> translateToScope(Scenario rawScenario, Map<String,Yolo.YoloDetection> observationYoloMap) {
        Point egoPosition = new Point(rawScenario.odometry().getX(), rawScenario.odometry().getY());

        Circle relevantMap = new Circle(egoPosition, 10000.0);
        Scope<ModelSeedFragment> scope = converter.getScope(relevantMap);
        scope.roadCutter(10.0);

//        Circle egoPlacement = new Circle(
//                egoPosition,
//                20.0);
        MapObject ego = new MapObject(1,  ObjectType.Car);
        scope.addEgo(ego);

        final List<Yolo.YoloDetection> yoloDetections = rawScenario.yolo().getYoloDetections();
        for(int i = 0; i<yoloDetections.size(); i++) {
            final var yoloDetection = yoloDetections.get(i);

            final Sector sector = new Sector(egoPosition, 25.0,
                    rawScenario.odometry().getTheta()+yoloDetection.angle(),
                    Math.PI/12);
            final ArrayList<ObjectType> types = new ArrayList<>();
            types.add(ObjectType.Signal);
            LinkedHashMap<Integer, MapObject> objects2  = scope.getMapObjects(sector, types);

            var observation = scope.addObjectObservations(objects2, "yolo_"+i, ObjectType.Signal);
            observationYoloMap.put(observation.getId(),yoloDetection);
        }
        return scope;
    }
}
