package space.vampir.engine.verification;

import tools.refinery.mapconverter.map.ObjectType;
import tools.refinery.mapconverter.scope.Scope;
import tools.refinery.mapconverter.transform.ModelSeedFragment;

import java.util.ArrayList;
import java.util.List;

public abstract class ObjectSelection {
    final protected double minimalDistance = 3.0;
    final Scope<ModelSeedFragment> scope;

    public ObjectSelection(Scope<ModelSeedFragment> scope) {
        this.scope = scope;
    }

    public abstract List<ObjectSelectionRecord> getObjects(
            double posX, double posY, double theta,
            double posError, double thetaError,
            double maxDistance,
            ObjectType targetType);
}
