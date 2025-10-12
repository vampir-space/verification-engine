package space.vampir.engine.message;

import java.util.ArrayList;
import java.util.List;

public class PointPillars extends Message {
    final List<PointPillarsDetection> detections;

    public PointPillars(long time, List<PointPillarsDetection> detections) {
        super(time);
        this.detections = detections;
    }

    public record PointPillarsDetection(double posX, double posY, double theta, double sizeX, double sizeY) {
    }

    public static PointPillars fromMap(Object map) {
        long time = readTime(map);
        List<PointPillarsDetection> detections = new ArrayList<>();

        for (var detection : readArray(map, "detections")) {
            var bbox = readMap(detection, "bbox");
            var center = readMap(bbox, "center");
            var position = readMap(center, "position");
            double posX = (double) readMap(position, "y");
            double posY = (double) readMap(position, "x");
            var quat = readMap(center,"orientation");
            var euler = MathUtility.quaternionToEuler(
                    (double) readMap(quat,"x"),
                    (double) readMap(quat,"y"),
                    (double) readMap(quat,"z"),
                    (double) readMap(quat,"w"));
            double theta = Math.toRadians(euler[2]) + Math.PI/2;

            var size = readMap(bbox, "size");
            var sizeX = (double) readMap(size, "x");
            var sizeY = (double) readMap(size, "y");

            detections.add(new PointPillarsDetection(posX, posY, theta, sizeX, sizeY));
        }

        return new PointPillars(time, detections);
    }

    public List<PointPillarsDetection> getDetections() {
        return detections;
    }
}
