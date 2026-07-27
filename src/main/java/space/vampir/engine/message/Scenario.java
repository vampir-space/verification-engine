package space.vampir.engine.message;

public record Scenario(
        long time,
        long timeInterval,
        Odometry odometry,
        boolean isInsideMapBorder,
        PointPillars pointPillars,
        Yolo yolo
) {

    public Scenario(Odometry odometry, boolean isInsideMapBorder, PointPillars pointPillars, Yolo yolo) {
        this(getTime(odometry, pointPillars, yolo),getInterval(odometry,pointPillars,yolo), odometry, isInsideMapBorder, pointPillars, yolo);
    }

    private static long getTime(Message... messages) {
        long time = Long.MAX_VALUE;
        for (var message : messages) {
            if (message != null) {
                time = Math.min(time, message.time);
            }
        }
        return time;
    }
    private static long getInterval(Message... messages) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (var message : messages) {
            if (message != null) {
                min = Math.min(min, message.time);
                max = Math.max(max, message.time);
            }
        }
        return max-min;
    }
}
