package space.vampir.engine.communication;

import space.vampir.engine.NoiseApplier;
import space.vampir.engine.communication.StateRecorder.SynchronizedMessages;
import space.vampir.engine.message.NavSat;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.VerificationCase;
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

        public RealScenarioProvider(MapRender mapRender) {
            this.mapRender = mapRender;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
//            System.out.println(sync);
            final Odometry lowEndOdometry;
            if (sync.lowEndGps() == null) {
                throw new IllegalArgumentException("No lowEndGps found");
            } else {
                lowEndOdometry = new Odometry(sync.lowEndGps().getTime(),
                        sync.lowEndGps().getLat(),
                        sync.lowEndGps().getLon(),
                        sync.imu().getTheta(),
                        sync.lowEndGps().getPositionCovariance()
                );
            }
            var navSat = sync.groundTruthGps();
            Scenario scenario = new Scenario(lowEndOdometry, isInBorder(mapRender, navSat), sync.pointPillars(), sync.yolo());

            Odometry gtOdometry = new Odometry(navSat.getTime(), navSat.getLat(), navSat.getLon(), sync.imu().getTheta(), navSat.getPositionCovariance());
            return new VerificationCase(scenario, gtOdometry);
        }
    }
}
