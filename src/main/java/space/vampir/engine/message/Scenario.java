package space.vampir.engine.message;

public record Scenario(long time, Odometry odometry, PointPillars pointPillars, Yolo yolo) {
    public Scenario(Odometry odometry, PointPillars pointPillars, Yolo yolo) {
        this(getTime(odometry,pointPillars,yolo), odometry, pointPillars, yolo);
    }
    private static long getTime(Message... messages) {
        long time = Long.MAX_VALUE;
        for(var message : messages) {
            if(message != null) {
                time = Math.min(time,message.time);
            }
        }
        return time;
    }
}
