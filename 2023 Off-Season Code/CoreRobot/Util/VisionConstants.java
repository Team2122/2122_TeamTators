package common.Util;

import java.io.IOException;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;

public class VisionConstants {
    // Change these values to match our robot.
    public static final Transform3d robotToCam =
            new Transform3d(
                    new Translation3d(-0.5, 0.0, 0.5), // Cam mounted half a meter forward of center, half a meter up from center.
                    new Rotation3d(0, 0, 0)); // Cam mounted facing forward.

    // Name of the camera in PhotonVision.
    public static final String cameraName = "OV5647";

    // The layout of the AprilTag field.
    public static AprilTagFieldLayout aprilTagFieldLayout;

    // The maximum pose ambiguity allowed before the pose is considered invalid.
    public static final double kMaxPoseAmbiguity = .5;

    // The maximum distance the robot can be from the target before the pose is considered invalid.
    public static final double maxDistanceMeters = 3;

    // Loads AprilTag field layout from file.
    static{
        try{
            aprilTagFieldLayout =  AprilTagFields.k2023ChargedUp.loadAprilTagLayoutField();
        } catch(IOException exception){
        }
    }
}
