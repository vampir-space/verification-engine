package space.vampir.engine.communication;

import space.vampir.engine.NoiseApplier;
import space.vampir.engine.communication.StateRecorder.SynchronizedMessages;
import space.vampir.engine.message.Odometry;
import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.VerificationCase;

public interface VerificationCaseProvider {

    VerificationCase getVerificationCase(SynchronizedMessages synchronizedMessages);

    class DummyNoiseOdometryProvider implements VerificationCaseProvider {

        private final double radiusStdDev;
        private final double thetaStdDev;

        public DummyNoiseOdometryProvider(double radiusStdDev, double thetaStdDev) {
            this.radiusStdDev = radiusStdDev;
            this.thetaStdDev = thetaStdDev;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            var navSat = sync.groundTruthGps();
            var imu = sync.imu();
            Odometry gtOdometry = new Odometry(navSat.getTime(), navSat.getLat(), navSat.getLon(), imu.getTheta(),navSat.getPositionCovariance());
            Odometry odometry = NoiseApplier.addNoise(gtOdometry, radiusStdDev, thetaStdDev);
            Scenario scenario = new Scenario(odometry, sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, gtOdometry);
        }
    }

    class NavSatOdometryProvider implements VerificationCaseProvider {
        final double noiseMultiplier;
        final double confidenceMultiplier;

        public NavSatOdometryProvider(double noiseMultiplier, double confidenceMultiplier) {
            this.noiseMultiplier = noiseMultiplier;
            this.confidenceMultiplier = confidenceMultiplier;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            final Odometry odometry;
            if(sync.lowEndGps() != null) {
                if(noiseMultiplier == 1.0) {
                    odometry = new Odometry(sync.lowEndGps().getTime(),
                            sync.lowEndGps().getLat(),
                            sync.lowEndGps().getLon(),
                            sync.imu().getTheta(),
                            sync.lowEndGps().getPositionCovariance()*confidenceMultiplier
                            //NoiseApplier.addGaussianNoise(sync.imu().getTheta(), thetaStdDev)
                    );
                } else {
                    var dif1 = sync.lowEndGps().getLat() - sync.groundTruthGps().getLat();
                    var dif2 = sync.lowEndGps().getLon() - sync.groundTruthGps().getLon();
                    odometry = new Odometry(sync.lowEndGps().getTime(),
                            sync.groundTruthGps().getLat() + dif1*noiseMultiplier,
                            sync.groundTruthGps().getLon() + dif2*noiseMultiplier,
                            sync.imu().getTheta(),
                            sync.lowEndGps().getPositionCovariance()*confidenceMultiplier
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
            Scenario scenario = new Scenario(odometry, sync.pointPillars(), sync.yolo());
            var navSat = sync.groundTruthGps();
            Odometry gtOdometry = new Odometry(navSat.getTime(), navSat.getLat(), navSat.getLon(), sync.imu().getTheta(), navSat.getPositionCovariance());
            return new VerificationCase(scenario, gtOdometry);
        }
    }

    class RealScenarioProvider implements VerificationCaseProvider {

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
//            System.out.println(sync);
            final Odometry lowEndOdometry;
            if(sync.lowEndGps() == null) {
                throw new IllegalArgumentException("No lowEndGps found");
            }
            else {
                lowEndOdometry = new Odometry(sync.lowEndGps().getTime(),
                        sync.lowEndGps().getLat(),
                        sync.lowEndGps().getLon(),
                        sync.imu().getTheta(),
                        sync.lowEndGps().getPositionCovariance()
                );
            }
            Scenario scenario = new Scenario(lowEndOdometry, sync.pointPillars(), sync.yolo());
            var navSat = sync.groundTruthGps();
            Odometry gtOdometry = new Odometry(navSat.getTime(), navSat.getLat(), navSat.getLon(), sync.imu().getTheta(), navSat.getPositionCovariance());
            return new VerificationCase(scenario, gtOdometry);
        }
    }
}
