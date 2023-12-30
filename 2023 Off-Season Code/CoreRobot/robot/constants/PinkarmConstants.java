package frc.robot.constants;

public class PinkarmConstants {
	public static final boolean waitingEnabled = false;

	public class ArmHWConstants {
        public static final int stages = 3;
        public static final double massM1 = .5;
        public static final double massM2 = .5;
        public static final double massM3 = .5;

        public static final double lengthL1 = .2;
        public static final double lengthL2 = .2;
        public static final double lengthL3 = .2;

		public static final double armWidth = .1;

        public static final double kPExt = .05;
        public static final double kIExt = 0;
        public static final double kDExt = 0;

        public static final double kPRot = .005;
        public static final double kIRot = 0;
        public static final double kDRot = 0;

        public static final double reductionForRotation = 10;
        public static final double reductionForExtension = 10;
        public static final double rotationsPerMeter = .1;

        public static final int rotationLeaderID = 15;
        public static final int rotationFollowerID = 31;

        public static final int extensionLeaderID = 8;
        public static final int extensionFollowerID = 38;

        public static final int kLimitSensorID = 22;

		public static final double absoluteEncoderSafety = 0.0;
        public static final int encoderChannel = 6;

        public static final double gravityAngle = -Math.PI / 2;

        public static final double MOTOR_ROTATIONS_PER_THROUGHBORE_ROTATION = 10;
        public static final double MOTOR_ROTATIONS_PER_DEGREE = 60;

        public static final double MOTOR_ROTATIONS_PER_INCH = .69824;
	}

    public class ArmExtensionConstants {
        public static final double error = 2;
        public static final double idleInches = 1;
        public static final double floorPickCubeInches = -error/2.0;
        public static final double floorPickConeInches = -error/2.0;
        public static final double highPickInches = 33;
        public static final double hybridPlaceInches = -error/2.0;
        public static final double midPlaceConeInches = 21.5;
        public static final double midPlaceCubeInches = 19.5;
        public static final double highPlaceConeInches = 39.5;
        public static final double highPlaceCubeInches = 38;
        public static final double kMaxOutput = 0.4;
        public static final double kMaxOutputSlow = 0.2;

 
        public static final double defaultP = 0.3;
        public static final double midPlaceConeP = 0.3;
        public static final double midPlaceCubeP = 0.3;
        public static final double highPlaceConeP = 0.3;
        public static final double highPlaceCubeP = 0.3;

        public static final double holdPower = .03;
        public static final double holdPowerRetact = -.08;
    }

    public class ArmRotationConstants {
        public static final double idleDegrees = -4.5;
        public static final double floorPickCubeDegrees = -4.5;
        public static final double floorPickConeDegrees = -4.5;
        public static final double highPickDegrees = 45.21; // Needs to be calibrated
        public static final double hybridPlaceDegrees = -4.5;
        public static final double midPlaceConeDegrees = 40.21;
        public static final double midPlaceCubeDegrees = 45.21;
        public static final double highPlaceConeDegrees = 42;
        public static final double autoHighPlaceConeDegrees = 44;
        public static final double highPlaceCubeDegrees = 37.1;

        public static final double kMaxOutput = 0.2; // for PID

        public static final double hybridPlaceP = 0.01;
        public static final double midPlaceConeP = 0.02;
        public static final double midPlaceCubeP = 0.02;
        public static final double highPlaceConeP = 0.03;
        public static final double highPlaceCubeP = 0.03;
        
        public static final double error = 2;
        public static final double angleOffset = -38.78947864;
    }
}
