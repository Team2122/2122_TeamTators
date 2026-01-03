package frc.robot.subsystems.overwatch.pivot;

import edu.wpi.first.math.geometry.Rotation2d;

public final class PivotConstants {
    public static final double motorRotationsPerCANcoderRotation = 68.57142857;

    public static double motorAngleToCANcoderAngle(double motorAngle) {
        return motorAngle / motorRotationsPerCANcoderRotation;
    }

    public static double CANcoderAngleToMotorAngle(double armAngle) {
        return armAngle * motorRotationsPerCANcoderRotation;
    }

    public static final Rotation2d ALLOWED_ERROR = Rotation2d.fromDegrees(2);
}
