package space.vampir.engine.verification;

import tools.refinery.mapconverter.map.MapObject;
import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.ModelSeedFragment;

import java.util.LinkedHashMap;

public class ObjectSelection {
    final Scope<ModelSeedFragment> scope;

    public ObjectSelection(Scope<ModelSeedFragment> scope) {
        this.scope = scope;
    }

    public LinkedHashMap<Integer, MapObject> getObjects(
            double posX, double posY, double theta,
            double posError, double thetaError,
            double maxDistance,
            ObjectType targetType) {
        LinkedHashMap<Integer, MapObject> selected = new LinkedHashMap<>();
        var objects = scope.getAllElements().getObjects();

        double backDistance = posError / Math.tan(thetaError);
        double backPositionX = posX - backDistance * Math.cos(theta);
        double backPositionY = posY - backDistance * Math.sin(theta);

        for (var object : objects.entrySet()) {
            if (object.getValue().getType() == targetType) {
                double targetX = object.getValue().getCoordinate().getX();
                double targetY = object.getValue().getCoordinate().getY();

                // if inside posError circle, then it is ok
                var distance = Math.sqrt((posX - targetX) * (posX - targetX) + (posY - targetY) * (posY - targetY));
                if (distance <= posError) {
                    selected.put(object.getKey(), object.getValue());
                }
                // otherwise, if it is not too far, we can check if it is visible
                else if (distance <= maxDistance + posError) {

                    double backToObjectY = targetY - backPositionY;
                    double backToObjectX = targetX - backPositionX;
                    double angle = Math.atan2(backToObjectY, backToObjectX);
                    double angleDiff = theta - angle;
//                    while (angleDiff < 0) {
//                        angleDiff += Math.PI * 2;
//                    }
//                    angleDiff = angleDiff % (Math.PI*2);
                    while (angleDiff < -Math.PI) {
                        angleDiff += Math.PI * 2;
                    }
                    while (angleDiff > Math.PI) {
                        angleDiff -= Math.PI * 2;
                    }

                    if (Math.abs(angleDiff) <= thetaError) {
                        if (Math.sqrt(backToObjectX * backToObjectX + backToObjectY * backToObjectY) > backDistance) {
                            selected.put(object.getKey(), object.getValue());
                        }

                    }
                }
            }
        }
        return selected;
    }
}
