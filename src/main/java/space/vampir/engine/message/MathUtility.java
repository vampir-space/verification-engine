package space.vampir.engine.message;

public class MathUtility {
    public static double[] quaternionToEuler(double quatX, double quatY, double quatZ, double quatW) {
        // roll (x-axis rotation)
        double sinRollCosPitch = 2 * (quatW * quatX + quatY * quatZ);
        double cosRollCosPitch = 1 - 2 * (quatX * quatX + quatY * quatY);
        double roll = Math.atan2(sinRollCosPitch, cosRollCosPitch);

        // pitch (y-axis rotation)
        double sinPitch = 2 * (quatW * quatY - quatZ * quatX);
        double pitch;
        if (Math.abs(sinPitch) >= 1) {
            pitch = Math.copySign(Math.PI / 2, sinPitch); // use 90 degrees if out of range
        } else {
            pitch = Math.asin(sinPitch);
        }

        // yaw (z-axis rotation)
        double sinYawCosPitch = 2 * (quatW * quatZ + quatX * quatY);
        double cosYawCosPitch = 1 - 2 * (quatY * quatY + quatZ * quatZ);
        double yaw = Math.atan2(sinYawCosPitch, cosYawCosPitch);

        return new double[] {
                Math.toDegrees(roll),
                Math.toDegrees(pitch),
                Math.toDegrees(yaw)
        };
    }
}
