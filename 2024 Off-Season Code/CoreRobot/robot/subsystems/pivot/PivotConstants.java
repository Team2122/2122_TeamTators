package frc.robot.subsystems.pivot;

public abstract class PivotConstants {
    public enum PivotPositions {
        HOME(20.5),
        AMP(59),
        TRAP(30),
        EJECT(50),
        FIXEDSHOT(31);

        public final double kDegrees;
        PivotPositions(double angle) {
            kDegrees = angle;
        }
    }

    public static final double homeHoldPower = -0.7;

    public static double kErrorDegrees = 0.5;

    public static final int kLeaderID = 58;
    public static final int kFollowerID = 59;
    public static final int kCancoderID = 0;

    public static final double kFallbackEncoderValue = 0.5;
    public static final double kMotorRotationsPerEncoderRotation = 119.97;
    public static final double kInitTimeout = 0.5;

    public static final double kDegreesPerMotorRotation = 1.0;
    public static double degreesToRotations(double degrees) {
        return degrees / kDegreesPerMotorRotation;
    }
    public static double rotationsToDegrees(double rotations) {
        return rotations * kDegreesPerMotorRotation;
    }

    public static double absoluteEncoderToRelativeEncoder(double value) {
        return value * kMotorRotationsPerEncoderRotation;
    }

    public static double relativeEncoderToAbsoluteEncoder(double value) {
        return value / kMotorRotationsPerEncoderRotation;
    }

    public static double kHardMinTarget = 20.5;
    public static double kHardMaxTarget = 59;
}
