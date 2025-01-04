package frc.robot.subsystems.climber;

public class ClimberConstants {
    public static final int kLeaderID      = 51;
    public static final int kFollowerID    = 52;
    public static final int kDownSensorID  = 23;
    public static final int kUpSensorID    = 1;
    public static final int kCancoderID    = 0;

    public static final double kCancoderOffset = 0;

    public static final double kGearing    = 1;

    public static final double kErrorDegrees = 1;

    public static final double kInitTimeout = 0.5;
    public static final double kFallbackEncoderValue = 0;

    public static final double kCancoderRotationsPerMotorRotation = 1.0;
    public static double cancoderToRelativeEncoder(double value) {
        return value / kCancoderRotationsPerMotorRotation;
    }
}
