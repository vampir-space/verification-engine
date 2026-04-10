package space.vampir.engine.verification;

public class VerificationEngineConfiguration {
    double relevantMapSegmentSize = 1000;
    boolean doRoadCutting = false;
    double roadCutterGranularity = 10;

    double gnssConfidenceRange=5;
    double yoloRange=50;
    double yoloAngleOfView=0.01;
    //double yoloAngleError=Math.PI/12;
    double yoloMinConfidence = 0.4;
}
