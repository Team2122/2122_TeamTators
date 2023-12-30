package frc.robot.constants;

public class SwerveConstants {
    private SwerveConstants() {
    }

    public static class SwerveInputProxy {
        public static int babyMilkID = 4;
        
        public static double maxVelocity = 5 * 1.3 * (4500.0 / 3500.0);
        public static double userRotationScalar = -2 * 1 * 1.2;
        public static double computerRotationScalar = -2 * 1;
        public static double turboModeVelocity = 5 * 1.37 * (4500.0 / 3500.0);
        public static double climberMode = 5 * 1.3 * (1200 / 3500.0);

        public static double coneDistance = 1.74 ;     // Offset for Cone extension during Auto's removed ( 2*0.0254 )
        public static double cubeDistance = 2;
        
//        public static double coneAngleOffset = -20;  // Updated for cropped image
        public static double coneAngleOffset = -2.5;   // Updated for full frame image
        public static double cubeAngleOffset = -7.89;
    }

    public static class SwerveModule {
        // public static double MOTOR_CORRECTION_FACTOR = 1.0667; // Calculated 10-29-2023 using error driving marks
        public static double MOTOR_ROTATIONS_PER_MODULE_ROTATION = 12.8;
        public static double MOTOR_ROTATIONS_PER_WHEEL_ROTATION = 4.72 ; // Calibrated 10-29-2023 based on mechanical cad
        public static double WHEEL_CIRCUMFERENCE = (.096) * Math.PI; // Measured wheel diameter 10-29-2023 with calipers
        public static double[] SWERVE_MODULE_OFFSETS = { 1, 1, 1, 1};
        // public static double[] SWERVE_MODULE_OFFSETS = { 1/5.81, 1/5.81, 1/5.81, 1/5.81};

        public static String canivoreBusName = "canbuse";
    }
}

