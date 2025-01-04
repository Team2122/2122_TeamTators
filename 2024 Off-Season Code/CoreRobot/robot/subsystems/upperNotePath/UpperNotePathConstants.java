package frc.robot.subsystems.upperNotePath;

public abstract class UpperNotePathConstants {
    public static final double kDunkerDropVoltage = -12;
    public static final double kDunkerStowVoltage = -12;
    public static final double kAmpDropTimeout = 4;
    public static final double kAmpStowingTimeout = 10;
    public static final double kEjectPrepTimeout = 1;
    public static final double kTrapDropTimeout = 4;
    public static final double kTrapAlmostOutTimeout = 4;
    public static final double kDunkerDeactivateTimeout = .2;
    public static final double kDiverterFlipVoltage = -1.75;
    public static final double kDiverterHoldVoltage = -0.3;
    public static final double kShooterErrorRPS = 1.5;

    public static final int kLeftShooterID = 56;
    public static final int kRightShooterID = 57;
    public static final int kDunkerMotorID = 0;
    public static final int kDunkerSensorChannel = 16;
    public static final int kDiverterLimitChannel = 15;
}
