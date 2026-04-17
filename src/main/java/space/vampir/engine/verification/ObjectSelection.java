package space.vampir.engine.verification;

import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.ModelSeedFragment;

import java.util.ArrayList;
import java.util.List;

public class ObjectSelection {
    final Scope<ModelSeedFragment> scope;

    public ObjectSelection(Scope<ModelSeedFragment> scope) {
        this.scope = scope;
    }

    public List<ObjectSelectionRecord> getObjects(
            double posX, double posY, double theta,
            double posError, double thetaError,
            double maxDistance,
            ObjectType targetType) {
        List<ObjectSelectionRecord> selected = new ArrayList<>();
        //LinkedHashMap<Integer, MapObject> selected = new LinkedHashMap<>();
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
                final double backToObjectY = targetY - backPositionY;
                final double backToObjectX = targetX - backPositionX;

                if (distance <= posError) {
                    final double angleDiff = calculateAngleDiff(targetX,targetY,backPositionX,backPositionY,theta);
                    selected.add(new ObjectSelectionRecord(angleDiff,object.getKey(),object.getValue()));
                }
                // otherwise, if it is not too far, we can check if it is visible
                else if (distance <= maxDistance + posError) {

                    double angleDiff = calculateAngleDiff(targetX,targetY,backPositionX,backPositionY,theta);

                    if (angleDiff <= thetaError) {
                        if (Math.sqrt(backToObjectX * backToObjectX + backToObjectY * backToObjectY) > backDistance) {
                            selected.add(new ObjectSelectionRecord(angleDiff,object.getKey(),object.getValue()));
                        }
                    }
                }
            }
        }
        return selected;
    }
    
    private double calculateAngleDiff(double targetX, double targetY, double backPositionX, double backPositionY, double theta) {
        final double backToObjectY = targetY - backPositionY;
        final double backToObjectX = targetX - backPositionX;
        final double angle = Math.atan2(backToObjectY, backToObjectX);
        double angleDiff = theta - angle;
        while (angleDiff < -Math.PI) {
            angleDiff += Math.PI * 2;
        }
        while (angleDiff > Math.PI) {
            angleDiff -= Math.PI * 2;
        }
        return Math.abs(angleDiff);
    }
}
