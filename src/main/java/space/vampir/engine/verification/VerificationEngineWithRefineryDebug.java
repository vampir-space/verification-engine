package space.vampir.engine.verification;

import org.jetbrains.annotations.NotNull;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.visualization.MapRender;
import tools.refinery.mapconverter.map.Converter;
import tools.refinery.mapconverter.map.MapHandler;
import tools.refinery.mapconverter.map.MapObject;
import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.*;
import tools.refinery.mapconverter.transform.*;

import java.io.IOException;
import java.util.*;

public class VerificationEngineWithRefineryDebug implements VerificationEngine{
    final Converter<String> converter;
    StringStrategy outputStrategy;
    ComplexityStrategy<String> complexityStrategy;
    MapGenerationProblemProvider problemProvider;

    // TODO: remove this dependency, reorganize
    final MapRender mapRender;

    public VerificationEngineWithRefineryDebug(MapHandler map, MapRender mapRender) throws IOException {
        this(map, mapRender, null);
    }

    public VerificationEngineWithRefineryDebug(MapHandler map, MapRender mapRender, String metamodelPath) throws IOException {
        outputStrategy = new StringStrategy();

        complexityStrategy = metamodelPath == null || metamodelPath.isBlank()
                ? new BasicWithTypeRefinementStrategy<>(outputStrategy)
                : new BasicWithTypeRefinementStrategy<>(outputStrategy, metamodelPath);
        converter = new Converter<>(map, complexityStrategy);

        problemProvider = new MapGenerationProblemProvider(complexityStrategy.getMetaModelString());

        this.mapRender = mapRender;
    }

    @Override
    public UpdatedScenario update(Scenario rawScenario) {
        Map<String, Yolo.YoloDetection> observationYoloMap = new HashMap<>();
        Scope<String> scope = translateToScope(rawScenario, observationYoloMap);


        String refineryFragment = scope.translateMap();

        System.out.println(refineryFragment);
        throw new UnsupportedOperationException();

    }


    private @NotNull Scope<String> translateToScope(Scenario rawScenario, Map<String,Yolo.YoloDetection> observationYoloMap) {
        // todo
        var xyCoords = mapRender.toMapCoord(rawScenario.odometry().getX(), rawScenario.odometry().getY());

        Point egoPosition = new Point(xyCoords[0], xyCoords[1]);

        Circle relevantMap = new Circle(egoPosition, 50.0);

        Scope<String> scope = converter.getScope(relevantMap);
        //scope.roadCutter(10.0);


////        Circle egoPlacement = new Circle(
////                egoPosition,
////                20.0);
//        final Circle egoCircle = new Circle(egoPosition,25.0);
//
//        final ArrayList<ObjectType> egoType = new ArrayList<>();
//        egoType.add(ObjectType.Car);
//        LinkedHashMap

//        complexityStrategy.setEgoRange(25);
//        MapObject car = new MapObject(1, ObjectType.Car, egoPosition, new Size(1.0, 1.0));
//        scope.addEgo(car);

        final List<Yolo.YoloDetection> yoloDetections = rawScenario.yolo().getYoloDetections();
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
