package space.vampir.engine.verification;

import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.ModelSeedFragment;

import java.util.ArrayList;
import java.util.List;

public class DistanceBasedObjectSelection extends ObjectSelection {
    DistanceBasedObjectSelection(Scope<ModelSeedFragment> scope) {
        super(scope);
    }

    public List<ObjectSelectionRecord> getObjects(double posX, double posY, double theta, double posError, double thetaError, double maxDistance, ObjectType targetType) {
        List<ObjectSelectionRecord> selected = new ArrayList<>();

        var objects = scope.getAllElements().getObjects();

        for (var object : objects.entrySet()) {
            if (object.getValue().getType() == targetType) {
                double targetX = object.getValue().getCoordinate().getX();
                double targetY = object.getValue().getCoordinate().getY();

                // if inside posError circle, then it is ok
                var distance = Math.sqrt((posX - targetX) * (posX - targetX) + (posY - targetY) * (posY - targetY));

                if (minimalDistance < distance && distance <= maxDistance + posError) {

                    final Double diff = calculateDiff(targetX, targetY, posX, posY, theta);
                    if (diff != null && diff < posError) {
                        selected.add(new ObjectSelectionRecord(diff, object.getKey(), object.getValue()));
                    }
                }
            }
        }
        return selected;
    }

    protected Double calculateDiff(double targetX, double targetY, double posX, double posY, double theta) {
        // move to origo
        final double targetXRelToPos = targetX-posX;
        final double targetYRelToPos = targetY-posY;
        // rotate
        final double rotatedX = targetXRelToPos*Math.cos(-theta)-targetYRelToPos*Math.sin(-theta);
        final double rotatedY = targetXRelToPos*Math.sin(-theta)+targetYRelToPos*Math.cos(-theta);

        if(rotatedX < this.minimalDistance) {
            return null;
        } else {
            return Math.abs(rotatedY);
        }



//// 1. Map displacements
//        double deltaX = targetX - posX;
//        double deltaY = targetY - posY;
//
//// 2. Project onto target angle axes (targetAngle relative to Map 0 in radians)
//        double dLong = deltaX * Math.sin(theta) + deltaY * Math.cos(theta);
//        double dLat  = deltaX * Math.cos(theta) - deltaY * Math.sin(theta);
//
//// 3. Sign is in front along targetAngle if dLong > 0
//        boolean isInFront = dLong > 0;
//
//        if (isInFront) {
//            // Exact lateral shift needed perpendicular to targetAngle
//            // Positive = shift in direction (+cos(phi), -sin(phi))
//            // Negative = shift in opposite direction
////            double lateralShiftNeeded = dLat;
//            return Math.abs(dLat);
//        }
//        else {
//            return  null;
//        }
    }
}
