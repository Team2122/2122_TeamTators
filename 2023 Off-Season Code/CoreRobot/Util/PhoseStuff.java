package common.Util;

import java.util.ArrayList;
import java.util.List;

import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.subsystems.SwerveDrive;

public class PhoseStuff implements Runnable {
    private SwerveDrive swerveDrive;
    private Vision vision;
    private SwerveDrivePoseEstimator poseEstimator;
    private Field2d fieldSim;

    public PhoseStuff(SwerveDrive swerveDrive, Vision vision) {
        this.swerveDrive = swerveDrive;
        this.vision = vision;

        // Creates the pose estimator.
        this.poseEstimator = new SwerveDrivePoseEstimator(
                swerveDrive.getKin(),
                swerveDrive.getGyroRotation2d(),
                swerveDrive.getSwerveModulePositions(),
                new Pose2d(new Translation2d(), Rotation2d.fromDegrees(0)),
                VecBuilder.fill(.1, .1, .1),
                VecBuilder.fill(.9, .9, .9));

        // this.fieldSim = new Field2d();
        // SmartDashboard.putData("Field", fieldSim);
    }

    @Override
    public void run() {
        tick();
    }

    // This is ran every tick.
    public void tick() {
        // Updates the pose estimator based on the gyro and swerve module positions.
        poseEstimator.update(
                swerveDrive.getGyroRotation2d(),
                swerveDrive.getSwerveModulePositions());

        // Vision calculations are ran here.
        // visionStuff();

        // fieldSim.setRobotPose(get());
    }

    // Gets the pose estimate.
    public Pose2d get() {
        return poseEstimator.getEstimatedPosition();
    }

    // Resets the pose estimator.
    public void reset(Pose2d pose) {
        poseEstimator.resetPosition(
                swerveDrive.getGyroRotation2d(),
                swerveDrive.getSwerveModulePositions(),
                pose);

        // fieldSim.setRobotPose(poseEstimator.getEstimatedPosition());
    }

    // Vision calculations are ran here.
    public void visionStuff() {
        // Get a new image from the camera.
        var pipelineResult = vision.getResult();

        // List of robot pose estimates that will be added to the pose estimator.
        List<Pose2d> robotEstimates = new ArrayList<>();

        // Variables for DEBUGGING PURPOSES ONLY.
        List<Pose2d> camEst = new ArrayList<>();
        List<Pose2d> robEst = new ArrayList<>();
        List<Pose2d> tags = new ArrayList<>();

        // Loop through all the AprilTag targets spotted in the image.
        for (PhotonTrackedTarget trackedTarget : Vision.getTargets(pipelineResult)) {
            // Gets the distance from the camera to the AprilTag.
            Transform3d camera_to_target = trackedTarget.getBestCameraToTarget();

            // Gets the AprilTag ID.
            int fiducialId = trackedTarget.getFiducialId();

            // Checks if the AprilTag ID exists.
            if (VisionConstants.aprilTagFieldLayout.getTagPose(fiducialId).isEmpty())
                continue;

            // Checks if the AprilTag error value is greater than the max error value.
            if (trackedTarget.getPoseAmbiguity() > VisionConstants.kMaxPoseAmbiguity)
                continue;

            // Checks if the AprilTag distance is greater than the max acceptable distance.
            if (hypot3d(camera_to_target.getX(), camera_to_target.getY(),
                    camera_to_target.getZ()) > VisionConstants.maxDistanceMeters)
                continue;

            // Gets the AprilTag pose and adds the AprilTag pose to the list. DEBUGGING PURPOSES ONLY.
            Pose3d fiducial_pose_3d = VisionConstants.aprilTagFieldLayout.getTagPose(fiducialId).get();
            tags.add(fiducial_pose_3d.toPose2d());

            // Gets the camera pose and adds the camera pose to the list. DEBUGGING PURPOSES ONLY.
            Pose3d camera_pose_3d_estimate = fiducial_pose_3d.transformBy(camera_to_target.inverse());
            camEst.add(camera_pose_3d_estimate.toPose2d());

            // Gets the robot pose and adds the robot pose to the list. DEBUGGING PURPOSES ONLY.
            Pose3d robot_pose_3d = camera_pose_3d_estimate.transformBy(VisionConstants.robotToCam.inverse());
            robEst.add(robot_pose_3d.toPose2d());

            // Adds the robot pose to the list. This is the one that is used for pose estimation.
            robotEstimates.add(robot_pose_3d.toPose2d());
        }

        // DEBUGGING methods for the Field2d. Use AdvantageScope to view this data.
        fieldSim.getObject("camera").setPoses(camEst);
        fieldSim.getObject("robos").setPoses(robEst);
        fieldSim.getObject("tags").setPoses(tags);

        // Gets the latency of the camera.
        double latencySeconds = Vision.getLatencySeconds(pipelineResult) / 1000 + 0.011;

        // Adds the vision measurements to the pose estimator.
        for (Pose2d estimated_robot_pose_2d : robotEstimates) {
            poseEstimator.addVisionMeasurement(estimated_robot_pose_2d, Timer.getFPGATimestamp() - latencySeconds);
        }
    }

    // stay in school kids
    private double hypot3d(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }
}
