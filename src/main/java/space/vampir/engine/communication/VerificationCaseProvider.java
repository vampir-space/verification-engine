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
            Odometry odometry = NoiseApplier.addNoise(sync.odometry(), radiusStdDev, thetaStdDev);
            Scenario scenario = new Scenario(odometry, sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, sync.odometry());
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
            if(sync.navSat() != null) {
                if(noiseMultiplier == 1.0) {
                    odometry = new Odometry(sync.navSat().getTime(),
                            sync.navSat().getLat(),
                            sync.navSat().getLon(),
                            sync.odometry().getTheta(),
                            sync.navSat().getPositionCovariance()*confidenceMultiplier
                            //NoiseApplier.addGaussianNoise(sync.odometry().getTheta(), thetaStdDev)
                    );
                } else {
                    var dif1 = sync.navSat().getLat() - sync.odometry().getX();
                    var dif2 = sync.navSat().getLon() - sync.odometry().getY();
                    odometry = new Odometry(sync.navSat().getTime(),
                            sync.odometry().getX() + dif1*noiseMultiplier,
                            sync.odometry().getY() + dif2*noiseMultiplier,
                            sync.odometry().getTheta(),
                            sync.navSat().getPositionCovariance()*confidenceMultiplier
                            //NoiseApplier.addGaussianNoise(sync.odometry().getTheta(), thetaStdDev)
                    );
                }
            } else {
                odometry = new Odometry(sync.odometry().getTime(),
                        sync.odometry().getX(),
                        sync.odometry().getY(),
                        sync.odometry().getTheta());
            }
            Scenario scenario = new Scenario(odometry, sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, sync.odometry());
        }
    }

    class RealScenarioProvider implements VerificationCaseProvider {

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            Scenario scenario = new Scenario(sync.odometry(), sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, sync.odometry());
        }
    }
}
