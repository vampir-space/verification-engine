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
    ComplexityStrategy<ModelSeedFragment> complexityStrategy;
    MapGenerationProblemProvider problemProvider;

    // TODO: remove this dependency, reorganize
    final MapRender mapRender;

    public VerificationEngineWithRefinery(MapHandler map, MapRender mapRender) throws IOException {
        this.outputStrategy = new ModelSeedStrategy();

        complexityStrategy = new BasicWithTypeRefinementStrategy<>(outputStrategy);
        converter = new Converter<>(map, complexityStrategy);

        problemProvider = new MapGenerationProblemProvider(complexityStrategy.getMetaModelString());

        this.mapRender = mapRender;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        Map<String,Yolo.YoloDetection> observationYoloMap = new HashMap<>();
        Scope<ModelSeedFragment> scope = translateToScope(rawScenario,observationYoloMap);

        ModelSeedFragment refineryFragment = scope.translateMap();
        ModelSeed modelSeed = refineryFragment.buildSeed();

        ModelGenerator model = outputStrategy.problemProvider.solve(modelSeed);

        // todo
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());
        GeometrySolver.OdometryPrior odometryPrior = new GeometrySolver.OdometryPrior(
                xyCoords[0],
                xyCoords[1],
                rawScenario.odometry().getTheta(),
                0.4);


        List<GeometrySolver.LocationDetection> locations = new ArrayList<>();
        locations.add(new GeometrySolver.LocationDetection(rawScenario.odometry().getX(), rawScenario.odometry().getY(), 10.0));

        List<GeometrySolver.YoloDetection> yolos = new ArrayList<>();

//        model.isLastGenerationSuccessful();
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


                yolos.add(new GeometrySolver.YoloDetection(landmarkCoordinate.getX(), landmarkCoordinate.getY(), observation.angle(), Math.PI/12));
            }
        }
        GeometrySolver.Solution geometrySolution = GeometrySolver.solve(odometryPrior,locations,yolos);

        var coordsInGeo = mapRender.toGeoCoord(geometrySolution.x, geometrySolution.y);

        return new UpdatedScenario(rawScenario, new Odometry(rawScenario.time(),coordsInGeo[0],coordsInGeo[1],geometrySolution.theta));
    }

    private @NotNull Scope<ModelSeedFragment> translateToScope(Scenario rawScenario, Map<String,Yolo.YoloDetection> observationYoloMap) {
        // todo
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());

        Point egoPosition = new Point(xyCoords[0], xyCoords[1]);

        Circle relevantMap = new Circle(egoPosition, 50.0);

        Scope<ModelSeedFragment> scope = converter.getScope(relevantMap);
        scope.roadCutter(10.0);


////        Circle egoPlacement = new Circle(
////                egoPosition,
////                20.0);
//        final Circle egoCircle = new Circle(egoPosition,25.0);
//
//        final ArrayList<ObjectType> egoType = new ArrayList<>();
//        egoType.add(ObjectType.Car);
//        LinkedHashMap

        complexityStrategy.setEgoRange(25);
        MapObject car = new MapObject(1, ObjectType.Car, egoPosition, new Size(1.0, 1.0));
        scope.addEgo(car);

        final List<Yolo.YoloDetection> yoloDetections = rawScenario.yolo() == null ? List.of() : rawScenario.yolo().getYoloDetections();
        for(int i = 0; i<yoloDetections.size(); i++) {
            final var yoloDetection = yoloDetections.get(i);

            final Sector sector = new Sector(egoPosition, 100.0,
                    rawScenario.odometry().getTheta()+yoloDetection.angle(),
                    Math.PI/12,
                    100.0,// TODO
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
