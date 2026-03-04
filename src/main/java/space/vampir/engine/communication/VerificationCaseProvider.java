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

        private final double thetaStdDev;

        public NavSatOdometryProvider(double thetaStdDev) {
            this.thetaStdDev = thetaStdDev;
        }

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            Odometry odometry = new Odometry(sync.navSat().getTime(),
                    sync.navSat().getLat(),
                    sync.navSat().getLon(),
                    NoiseApplier.addGaussianNoise(sync.odometry().getTheta(), thetaStdDev));
            Scenario scenario = new Scenario(odometry, sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, sync.odometry());
        }
    }

    class RealScenarioProvider implements VerificationCaseProvider {

        @Override
        public VerificationCase getVerificationCase(SynchronizedMessages sync) {
            Scenario scenario = new Scenario(sync.lowEndOdometry(), sync.pointPillars(), sync.yolo());
            return new VerificationCase(scenario, sync.odometry());
        }
    }
}
