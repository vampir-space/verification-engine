package space.vampir.engine.message;

public class NavSat extends Message {
    final double lat;
    final double lon;
    final double positionCovariance;

    public NavSat(long time, double lat, double lon, double positionCovariance) {
        super(time);
        this.lat = lat;
        this.lon = lon;
        this.positionCovariance = positionCovariance;
    }

    public static NavSat fromMap(Object map) {
        long time = readTime(map);

        double lat = (double) readMap(map,"latitude");
        double lon = (double) readMap(map,"longitude");

        var covarianceMap = readArray(map, "position_covariance");
        var cx = covarianceMap.get(0);
        var cy = covarianceMap.get(4);
        double positionCovariance = Math.max((Double) cx, (Double) cy);

        return new NavSat(time, lat, lon, positionCovariance);
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getPositionCovariance() {
        return this.positionCovariance;
    }
}
