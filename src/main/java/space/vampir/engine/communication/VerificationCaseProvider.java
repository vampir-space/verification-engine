package space.vampir.engine.communication;

import space.vampir.engine.NoiseApplier;
import space.vampir.engine.communication.StateRecorder.SynchronizedMessages;
import space.vampir.engine.message.NavSat;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.message.Yolo;
import space.vampir.engine.verification.VerificationCase;
import space.vampir.engine.verification.VerificationEngineConfiguration;
import space.vampir.engine.visualization.MapRender;

public interface VerificationCaseProvider {

    VerificationCase getVerificationCase(SynchronizedMessages synchronizedMessages);

    default boolean isInBorder(MapRender mapRender, NavSat gt) {
        return mapRender.isInsideBorder(gt.getLat(), gt.getLon());
    }

    class DummyNoiseOdometryProvider implements VerificationCaseProvider {
        private final double radiusStdDev;
        private final double thetaStdDev;
        final MapRender mapRender;

        public DummyNoiseOdometryProvider(MapRender mapRender, double radiusStdDev, double thetaStdDev) {
            this.mapRender = mapRender;
            this.radiusStdDev = radiusStdDev;
            this.thetaStdDev = thetaStdDev;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            var navSat = sync.groundTruthGps();
            var imu = sync.imu();
            Odometry gtOdometry = new Odometry(navSat.getTime(), navSat.getLat(), navSat.getLon(), imu.getTheta(), navSat.getPositionCovariance());
            Odometry odometry = NoiseApplier.addNoise(gtOdometry, radiusStdDev, thetaStdDev);
            Scenario scenario = new Scenario(odometry, isInBorder(mapRender, navSat), sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, gtOdometry);
        }
    }

    class NavSatOdometryProvider implements VerificationCaseProvider {
        final double noiseMultiplier;
        final double confidenceMultiplier;
        final MapRender mapRender;

        public NavSatOdometryProvider(MapRender mapRender, double noiseMultiplier, double confidenceMultiplier) {
            this.noiseMultiplier = noiseMultiplier;
            this.confidenceMultiplier = confidenceMultiplier;
            this.mapRender = mapRender;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            final Odometry odometry;
            if (sync.lowEndGps() != null) {
                if (noiseMultiplier == 1.0) {
                    odometry = new Odometry(sync.lowEndGps().getTime(),
                            sync.lowEndGps().getLat(),
                            sync.lowEndGps().getLon(),
                            sync.imu().getTheta(),
                            sync.lowEndGps().getPositionCovariance() * confidenceMultiplier
                            //NoiseApplier.addGaussianNoise(sync.imu().getTheta(), thetaStdDev)
                    );
                } else {
                    var dif1 = sync.lowEndGps().getLat() - sync.groundTruthGps().getLat();
                    var dif2 = sync.lowEndGps().getLon() - sync.groundTruthGps().getLon();
                    odometry = new Odometry(sync.lowEndGps().getTime(),
                            sync.groundTruthGps().getLat() + dif1 * noiseMultiplier,
                            sync.groundTruthGps().getLon() + dif2 * noiseMultiplier,
                            sync.imu().getTheta(),
                            sync.lowEndGps().getPositionCovariance() * confidenceMultiplier
                            //NoiseApplier.addGaussianNoise(sync.imu().getTheta(), thetaStdDev)
                    );
                }
            } else {
                odometry = new Odometry(sync.groundTruthGps().getTime(),
                        sync.groundTruthGps().getLat(),
                        sync.groundTruthGps().getLon(),
                        sync.imu().getTheta(),
                        0);
            }
            var navSat = sync.groundTruthGps();
            Scenario scenario = new Scenario(odometry, isInBorder(mapRender, navSat), sync.pointPillars(), sync.yolo());

            Odometry gtOdometry = new Odometry(navSat.getTime(), navSat.getLat(), navSat.getLon(), sync.imu().getTheta(), navSat.getPositionCovariance());
            return new VerificationCase(scenario, gtOdometry);
        }
    }

    class RealScenarioProvider implements VerificationCaseProvider {

        final MapRender mapRender;
        final VerificationEngineConfiguration veConfig;

        public RealScenarioProvider(MapRender mapRender, VerificationEngineConfiguration veConfig) {
            this.mapRender = mapRender;
            this.veConfig = veConfig;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
//            System.out.println(sync);
            final Odometry lowEndOdometry;
            var theta = sync.imu().getTheta();
            var yoloOffsetLongitudinal = veConfig.yoloOffsetLongitudinal;
            var yoloOffsetLateral = veConfig.yoloOffsetLateral;
            if (sync.lowEndGps() == null) {
                throw new IllegalArgumentException("No lowEndGps found");
            } else {
                var mapCoords = mapRender.toMapCoord(sync.lowEndGps().getLat(), sync.lowEndGps().getLon());
                var gnssOffsetLongitudinal = veConfig.gnssOffsetLongitudinal;
                var gnssOffsetLateral = veConfig.gnssOffsetLateral;
                double totalLongitudinal = gnssOffsetLongitudinal + yoloOffsetLongitudinal;
                double totalLateral = gnssOffsetLateral + yoloOffsetLateral;
                mapCoords[0] += totalLongitudinal * Math.sin(theta) + totalLateral *  Math.cos(theta);
                mapCoords[1] += totalLongitudinal * Math.cos(theta) - totalLateral *  Math.sin(theta);
                var geoCoords = mapRender.toGeoCoord(mapCoords[0], mapCoords[1]);
                lowEndOdometry = new Odometry(sync.lowEndGps().getTime(),
                        geoCoords[0],
                        geoCoords[1],
                        theta,
                        sync.lowEndGps().getPositionCovariance()
                );
            }
            var navSat = sync.groundTruthGps();
            var yolo = sync.yolo();
            if (yolo != null) {
                yolo.getYoloDetections().replaceAll(yoloDetection -> new Yolo.YoloDetection(yoloDetection.type(), yoloDetection.angle() + Math.toRadians(veConfig.yoloOffsetAngle), yoloDetection.confidence()));
            }
            Scenario scenario = new Scenario(lowEndOdometry, isInBorder(mapRender, navSat), sync.pointPillars(), yolo);

            var gtMapCoords = mapRender.toMapCoord(navSat.getLat(), navSat.getLon());
            gtMapCoords[0] += yoloOffsetLongitudinal * Math.sin(theta) + yoloOffsetLateral * Math.cos(theta);
            gtMapCoords[1] += yoloOffsetLongitudinal * Math.cos(theta) - yoloOffsetLateral * Math.sin(theta);
            var gtGeoCoords = mapRender.toGeoCoord(gtMapCoords[0], gtMapCoords[1]);
            Odometry gtOdometry = new Odometry(navSat.getTime(), gtGeoCoords[0], gtGeoCoords[1], sync.imu().getTheta(), navSat.getPositionCovariance());
            return new VerificationCase(scenario, gtOdometry);
        }
    }
}
