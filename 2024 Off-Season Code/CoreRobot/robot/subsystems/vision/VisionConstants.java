package frc.robot.subsystems.vision;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.*;

public abstract class VisionConstants {
    /** Represents the observation of a simple target. Not used for pose estimation */
    public static record TargetObservation(double distToTag, Rotation2d tx, Rotation2d ty, int id) {}

    /** Represents a robot pose sample used for pose estimation */
    public static record PoseObservation(
        double timestamp,
        Pose3d pose,
        double ambiguity,
        int tagCount,
        double averageTagDistance,
        PoseObservationType type) {}

    public static enum PoseObservationType {
        MEGATAG1(1.05, 1.0, 1.0),
        MEGATAG2(1.1, 1.1, Double.MAX_VALUE),
        PHOTONVISION(1, 1, 1);

        public final double stdDevFactorX;
        public final double stdDevFactorY;
        public final double stdDevFactorTheta;

        private PoseObservationType(double stdDevFactorX, double stdDevFactorY, double stdDevFactorTheta) {
            this.stdDevFactorX = stdDevFactorX;
            this.stdDevFactorY = stdDevFactorY;
            this.stdDevFactorTheta = stdDevFactorTheta;
        }
    }

    public static final AprilTagFieldLayout TAG_LAYOUT =
        AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField);

    public static final float MAX_AMBIGUITY = 0.3f;
    public static final double MAX_Z_ERROR = 0.75;

    public static final double LINEAR_STD_DEV_BASE = .2; // meters
    public static final double ANGULAR_STD_DEV_BASE = 0.2; // radians

    public static final double[] CAMERA_STD_DEV_FACTORS = {1, 1, 1, 1};

    public static final int FRONT_CAM_INDEX = 0;

    public static final int BLUE_SPEAKER_ID = 7;
    public static final int RED_SPEAKER_ID = 4;

    public static final Transform3d LEFT_CAM_POS = new Transform3d(
        -0.18415, -0.26035, 0.3429, new Rotation3d(0, 20, 90)
    );
    public static final Transform3d RIGHT_CAM_POS = new Transform3d(
        0.2413, 0.26035, 0.3429, new Rotation3d(0, 20, 270)
    );
    public static final Transform3d FRONT_CAM_POS = new Transform3d(
        0.2225, .145, 0.22, new Rotation3d(180, 20, 0)
    );
    public static final Transform3d BACK_CAM_POS = new Transform3d(
        -0.1778, -0.127, 0.32, new Rotation3d(0, 20, 180)
    );
}
