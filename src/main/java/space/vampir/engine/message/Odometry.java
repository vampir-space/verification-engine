package space.vampir.engine.message;

import java.util.Map;

public class Odometry extends Message{
    final double x;
    final double y;
    final double theta;

    public Odometry(long time, double x, double y, double theta) {
        super(time);
        this.x = x;
        this.y = y;
        this.theta = theta;
    }

    public static Odometry fromMap(Object map) {
        var timeStamp = readMap(readMap(map,"header"),"stamp");
        int sec = (int) readMap(timeStamp,"sec");
        int nano = (int) readMap(timeStamp,"nanosec");
        long time = sec*1000000000l+nano;

        Object pose = readMap(readMap(map, "pose"), "pose");
        var coords = readMap(pose,"position");
        double x = (double) readMap(coords,"x");
        double y = (double) readMap(coords,"y");
        double theta = ((double) readMap(readMap(pose,"orientation"),"z"))*Math.PI + Math.PI/2;
        System.out.println(readMap(pose,"orientation"));


        return new Odometry(time,x,y,theta);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getTheta() {
        return theta;
    }
}
