package space.vampir.engine.visualization;

import space.vampir.engine.message.Scenario;

import java.net.URL;

public class SceneVisualization {
    protected static URL lineImage = SceneVisualization.class.getResource("/line.svg");
    protected static URL lineImage2 = SceneVisualization.class.getResource("/line2.svg");
    protected static URL carImage = SceneVisualization.class.getResource("/car.svg");
    protected static URL egoImage = SceneVisualization.class.getResource("/ego.svg");

    final MapRender map;

    final ObjectRender ego = new ObjectRender(egoImage,
            3,5,0,0,0);
    final ObjectRender circle = new ObjectRender(RenderExample.class.getResource("/blue-circle.svg"), 30,30,0,0,0);

    public SceneVisualization(MapRender map) {
        this.map = map;
    }

    public synchronized void show(Scenario state) {
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
                        ego.getX() + detection.posX()*cosT + detection.posY()*sinT,
                        ego.getY() + detection.posX()*sinT - detection.posY()*cosT,
                        detection.theta()+ ego.getTheta());

//                var o = new ObjectRender(
//                        carImage,
//                        detection.sizeY(),
//                        detection.sizeX(),
//                        detection.posY(),
//                        detection.posX(),
//
//                        detection.theta()
//                );
                map.addObject(o);
            }
        }
    }
}
