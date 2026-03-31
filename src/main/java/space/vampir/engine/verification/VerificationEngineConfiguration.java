package space.vampir.engine.verification;

public class VerificationEngineConfiguration {
    double relevantMapSegmentSize = 1000;
    boolean doRoadCutting = false;
    double roadCutterGranularity = 10;

    double gnssConfidenceRange=5;
    double yoloRange=30;
    double yoloAngleOfView=Math.PI/120;
    //double yoloAngleError=Math.PI/12;

    double odometryConfidence = 0.4;
//    double

}
