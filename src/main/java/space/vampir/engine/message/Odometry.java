package space.vampir.engine.message;

public class Odometry extends Message{
    final double x;
    final double y;
    final double theta;
    final double uncertaintyInMeters;

    public Odometry(long time, double x, double y, double theta, double uncertaintyInMeters) {
        super(time);
        this.x = x;
        this.y = y;
        this.theta = theta;
        this.uncertaintyInMeters = uncertaintyInMeters;
    }

//    public Odometry(long time, double x, double y, double theta) {
//        this(time,x,y,theta,0);
//    }

//    public static Odometry fromMap(Object map) {
//        long time = readTime(map);
//
//        Object pose = readMap(readMap(map, "pose"), "pose");
//        var coords = readMap(pose,"position");
//        double x = (double) readMap(coords,"x");
//        double y = (double) readMap(coords,"y");
//        var quat = readMap(pose,"orientation");
//        var euler = MathUtility.quaternionToEuler(
//                (double) readMap(quat,"x"),
//                (double) readMap(quat,"y"),
//                (double) readMap(quat,"z"),
//                (double) readMap(quat,"w"));
//        double theta = Math.toRadians(euler[2]) + Math.PI/2;
//
//
//        return new Odometry(time,x,y,theta);
//    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getTheta() {
        return theta;
    }

    public double getUncertaintyInMeters() {
        return uncertaintyInMeters;
    }
}
