package space.vampir.engine.message;

public class Imu extends Message {

    final double theta;
    public Imu(long time, double theta) {
        super(time);
        this.theta = theta;
    }

    public static Imu fromMap(Object map) {
        long time = readTime(map);

        var quat = readMap(map,"orientation");
        var euler = MathUtility.quaternionToEuler(
                (double) readMap(quat,"x"),
                (double) readMap(quat,"y"),
                (double) readMap(quat,"z"),
                (double) readMap(quat,"w"));
        double theta = -(Math.toRadians(euler[2]) - Math.PI/2);

        return new Imu(time,theta);
    }

    public double getTheta() {
        return theta;
    }
}
