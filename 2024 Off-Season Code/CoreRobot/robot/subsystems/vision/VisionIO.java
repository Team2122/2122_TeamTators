package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import frc.robot.subsystems.vision.VisionConstants.PoseObservation;
import frc.robot.subsystems.vision.VisionConstants.TargetObservation;
import org.littletonrobotics.junction.AutoLog;

public interface VisionIO {
    @AutoLog
    public class VisionIOInputs {
        protected boolean connected = true;
        protected TargetObservation[] latestTargetObservations = new TargetObservation[] {};
        protected PoseObservation[] poseObservations = new PoseObservation[] {};
    }

    public default void updateInputs(VisionIOInputs inputs) {}

    public default void setOrientation(Rotation2d yaw, Rotation2d yawRate) {}
}
