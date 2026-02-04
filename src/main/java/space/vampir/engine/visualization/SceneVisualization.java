package space.vampir.engine.visualization;

import space.vampir.engine.message.Scenario;
import space.vampir.engine.verification.UpdatedScenario;

import java.net.URL;

public class SceneVisualization {
    protected static URL lineImage = SceneVisualization.class.getResource("/line.svg");
    protected static URL lineImage2 = SceneVisualization.class.getResource("/line2.svg");
    protected static URL carImage = SceneVisualization.class.getResource("/car.svg");
    protected static URL egoImage = SceneVisualization.class.getResource("/ego.svg");
    protected static URL laneImage = SceneVisualization.class.getResource("/lane.svg");
    protected static URL gnssImage = SceneVisualization.class.getResource("/gnss.svg");
    protected static URL veImage = SceneVisualization.class.getResource("/ve.svg");
    protected static URL gtImage = SceneVisualization.class.getResource("/gt.svg");



    final MapRender map;

    final ObjectRender ego = new ObjectRender(egoImage, 3,5,0,0,0);
    final ObjectRender circle = new ObjectRender(RenderExample.class.getResource("/blue-circle.svg"), 30,30,0,0,0);
    final ObjectRender gnss = new ObjectRender(gnssImage, 3,5,0,0,0);
    final ObjectRender ve = new ObjectRender(veImage, 3,5,0,0,0);
    final ObjectRender gt = new ObjectRender(gtImage,3,5,0,0,0);

    public SceneVisualization(MapRender map) {
        this.map = map;
    }

    public synchronized void show(Scenario state){
        map.getObjects().clear();
        var odom = state.odometry();
        if(odom != null) {
            var coord = map.toMapCoord(odom.getX(),odom.getY());

            ego.setX(coord[0]);
            ego.setY(coord[1]);
            ego.setTheta(odom.getTheta());
            circle.setX(coord[0]);
            circle.setY(coord[1]);

            map.addObject(ego);
            map.addObject(circle);

            map.addObject(new ObjectRender(lineImage, circle.getSizeX(), circle.getSizeY(), ego.getX(), ego.getY(), ego.getTheta()));
        }

        var yolo = state.yolo();
        if(yolo != null) {
            for(var detection : yolo.getYoloDetections()) {
                final URL line;
                if(detection.type().equals("car")) {
                    line = lineImage;
                } else {
                    line = lineImage2;
                    System.out.println(detection.type());
                }

                var o = new ObjectRender(line,12,80, ego.getX(), ego.getY(), ego.getTheta()+detection.angle());
                map.addObject(o);
            }
        }

        var pointPillars = state.pointPillars();
        if(pointPillars != null) {
            for(var detection : pointPillars.getDetections()){

                var cosT = Math.cos(ego.theta);
                var sinT = Math.sin(ego.theta);
                var o = new ObjectRender(
                        carImage,
                        detection.sizeY(),
                        detection.sizeX(),
                        ego.getX() - detection.posX() * cosT + detection.posY() * sinT,
                        ego.getY() + detection.posX() * sinT + detection.posY() * cosT,
                        detection.theta() + ego.getTheta());
                map.addObject(o);
            }
        }
    }

    public synchronized void show(UpdatedScenario updatedScenario){
        map.getObjects().clear();

        Scenario state = updatedScenario.scenario();
        //Drawing car based on gnss
        var gnssOdom = state.odometry();
        if(gnssOdom != null) {
            var coord = map.toMapCoord(gnssOdom.getX(),gnssOdom.getY());

            gnss.setX(coord[0]);
            gnss.setY(coord[1]);
            gnss.setTheta(gnssOdom.getTheta());
            circle.setX(coord[0]);
            circle.setY(coord[1]);

            map.addObject(gnss);
            map.addObject(circle);

            map.addObject(new ObjectRender(lineImage, circle.getSizeX(), circle.getSizeY(), gnss.getX(), gnss.getY(), gnss.getTheta()));
        }

        //drawing car based on ve
        var veOdom = updatedScenario.updatedByVerificationEngine();
        if(veOdom != null) {
            var coord = map.toMapCoord(veOdom.getX(),veOdom.getY());

            ve.setX(coord[0]);
            ve.setY(coord[1]);
            ve.setTheta(veOdom.getTheta());
            map.addObject(ve);
        }

        //drawing car based on ve
        var gtOdom = updatedScenario.groundTruth();
        if(gtOdom != null) {
            var coord = map.toMapCoord(gtOdom.getX(),gtOdom.getY());

            gt.setX(coord[0]);
            gt.setY(coord[1]);
            gt.setTheta(gtOdom.getTheta());
            map.addObject(gt);
        }

        var yolo = state.yolo();
        if(yolo != null) {
            for(var detection : yolo.getYoloDetections()) {
                final URL line;
                if(detection.type().equals("car")) {
                    line = lineImage;
                } else {
                    line = lineImage2;
                    System.out.println(detection.type());
                }

                var o = new ObjectRender(line,12,80, gnss.getX(), gnss.getY(), gnss.getTheta()+detection.angle());
                map.addObject(o);
            }
        }

        var pointPillars = state.pointPillars();
        if(pointPillars != null) {
            for(var detection : pointPillars.getDetections()){

                var cosT = Math.cos(gnss.theta);
                var sinT = Math.sin(gnss.theta);
                var o = new ObjectRender(
                        carImage,
                        detection.sizeY(),
                        detection.sizeX(),
                        gnss.getX() - detection.posX() * cosT + detection.posY() * sinT,
                        gnss.getY() + detection.posX() * sinT + detection.posY() * cosT,
                        detection.theta() + gnss.getTheta());
                map.addObject(o);
            }
        }
    }
}
