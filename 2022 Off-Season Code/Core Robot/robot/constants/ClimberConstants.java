package frc.robot.constants;

public class ClimberConstants {
    public static final int kClimbSensorChannel = 17; // 0-15
    public static final int kStingerServoChannel = 3;
    public static final int kShuttleMotorID = 33;

    public static final double kRaiseShuttlePosition = 251;  // was 225
    public static final double kRaiseShuttlePower = 0.75 ;  // was 0.25

    public static final double kRetractShuttlePosition = 1.5;
    public static final double kShuttleErrorMargin = 1;
    public static final double kRetractShuttlePercentOutput = -1;
    
    public static final double kExtendShuttleTraversalPosition = 110;
    public static final double kExtendShuttleTraversalPercentOutput = 0.5;

    public static final double kResetSlowPower = -0.1;

    public static final double kAlmostHomedRotations = 0;

    public static final double kLowerMotorSlowPower = -0.06;
    public static final double kLowerMotorExtraSlowPower = - 0.01;
    public static final double kRaiseMotorSlowPower = 0.05;

    public static final double kServoPosition = 0;
    public static final double kServoAngleDegrees = 0;

    public static final double kShuttleControllerP = 0.15/2;
    public static final double kShuttleControllerI = 0;
    public static final double kShuttleControllerD = 0;
    public static final double kShuttleControllerIZone = 0;
    public static final double kShuttleControllerF = 0;

    // The actual amount of rotations between the wheel things is 20.6
    public static final double kHomingCheckRotations = 24; // this is wrong now

    //servo goes in at most 0.2 and out at most 1.

}
