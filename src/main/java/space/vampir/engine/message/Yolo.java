package space.vampir.engine.message;

import java.util.ArrayList;
import java.util.List;

public class Yolo extends Message{
    final List<YoloDetection> yoloDetections;

    public Yolo(long time, List<YoloDetection> yoloDetections) {
        super(time);
        this.yoloDetections = yoloDetections;
    }

    public record YoloDetection(String type, double angle, double confidence) {}

    public static Yolo fromMap(Object map) {
        long time = readTime(map);
        List<YoloDetection> yoloDetections = new ArrayList<>();

        var types = readArray(map,"type");
        var angles = readArray(map, "angle");
        var confidences = readArray(map, "confidence");

        int size = types.size();
        for(int i = 0; i<size; i++) {
            if(angles.size() == size && confidences.size() == size) {
                String type = (String) types.get(i);
                double reportedAngle = (double) angles.get(i);
                double angle = Math.PI*(reportedAngle/180);
                double confidence = (double) confidences.get(i);
                yoloDetections.add(new YoloDetection(type,angle,confidence));
            } else {
                System.out.println("X: " + size +" " + angles.size() +" " + confidences.size());
            }
        }

        return new Yolo(time, yoloDetections);
    }

    public List<YoloDetection> getYoloDetections() {
        return yoloDetections;
    }
}
