package space.vampir.engine.message;

public class NavSat extends Message {
    final double lat;
    final double lon;

    public NavSat(long time, double lat, double lon) {
        super(time);
        this.lat = lat;
        this.lon = lon;
    }

    public static NavSat fromMap(Object map) {
        long time = readTime(map);

        double lat = (double) readMap(map,"latitude");
        double lon = (double) readMap(map,"longitude");

        return new NavSat(time, lat, lon);
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
