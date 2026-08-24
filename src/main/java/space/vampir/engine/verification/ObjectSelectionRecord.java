package space.vampir.engine.verification;

import tools.refinery.mapconverter.map.MapObject;

public class ObjectSelectionRecord {
    double diff;
    boolean detectionCompatible;
    Integer id;
    MapObject mapObject;

    public ObjectSelectionRecord(double diff, Integer id, MapObject mapObject) {
        this(diff,id,mapObject,true);
    }

    public ObjectSelectionRecord(double diff, Integer id, MapObject mapObject, boolean detectionCompatible) {
        this.diff = diff;
        this.id = id;
        this.mapObject = mapObject;
        this.detectionCompatible = detectionCompatible;
    }

    public double diff() {
        return diff;
    }
    public boolean detectionCompatible() {
        return detectionCompatible;
    }

    public void setDetectionCompatible(boolean detectionCompatible) {
        this.detectionCompatible = detectionCompatible;
    }

    public Integer id() {
        return id;
    }
    public MapObject mapObject() {
        return mapObject;
    }
}
