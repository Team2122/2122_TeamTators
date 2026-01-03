package frc.robot.subsystems.vision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;

public abstract class VisionConstants {

    public static record TargetObservation(double distToTag, Rotation2d tx, Rotation2d ty, int id) {}

    public static final AprilTagFieldLayout TAG_LAYOUT =
            AprilTagFieldLayout.loadField(AprilTagFields.k2025ReefscapeWelded);

    public static record PoseObservation(
            double timestamp,
            Pose3d pose,
            double ambiguity,
            int tagCount,
            double averageTagDistance,
            PoseObservationType type) {}

    public static enum PoseObservationType {
        MEGATAG1(1.05, 1.0, 100.0),
        MEGATAG2(1.1, 1.1, 100.0),
        PHOTONVISION(1, 1, 100.0);

        public final double stdDevFactorX;
        public final double stdDevFactorY;
        public final double stdDevFactorTheta;

        private PoseObservationType(
                double stdDevFactorX, double stdDevFactorY, double stdDevFactorTheta) {
            this.stdDevFactorX = stdDevFactorX;
            this.stdDevFactorY = stdDevFactorY;
            this.stdDevFactorTheta = stdDevFactorTheta;
        }
    }

    public static final float MAX_AMBIGUITY = 0.3f;
    public static final double MAX_Z_ERROR = 0.75;

    public static final double LINEAR_STD_DEV_BASE = .2; // meters
    public static final double ANGULAR_STD_DEV_BASE = 0.2; // radians

    public static final double[] CAMERA_STD_DEV_FACTORS = {1, 1, 1};

    public static final int FRONT_CAM_INDEX = 0;

    public static final String LEFT_CAM_NAME = "limelight-left";
    public static final String RIGHT_CAM_NAME = "limelight-right";
}
