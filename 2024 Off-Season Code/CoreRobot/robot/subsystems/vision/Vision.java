package frc.robot.subsystems.vision;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import frc.robot.Robot;
import frc.robot.Robot.RobotControlMode;
import frc.robot.constants.GeneralConstants;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.vision.VisionConstants.PoseObservationType;
import frc.robot.subsystems.vision.VisionConstants.TargetObservation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.Subsystem;

public class Vision extends Subsystem {
    private VisionIO[] io;
    private VisionIOInputsAutoLogged[] inputs;

    private SwerveDrive swerve;

    public Vision() {
        io = new VisionIO[4];
        inputs = new VisionIOInputsAutoLogged[io.length];
        for (int i = 0; i < io.length; i++) {
            io[i] = switch(GeneralConstants.kRobotMedium) {
                case REAL -> new VisionIOLimelight(indexToName(i));
                case SIM, REPLAY -> new VisionIO(){};
            };
            inputs[i] = new VisionIOInputsAutoLogged();
        }
        setValidity(
            RobotControlMode.Autonomous,
            RobotControlMode.Teleop,
            RobotControlMode.Disabled);
    }

    @Override
    public void configure() {
        this.swerve = Robot.getSwerve();
    }

    @Override
    public void log() {
        for (int i = 0; i < io.length; i++) {
            io[i].setOrientation(swerve.getYawBlueOrigin(), swerve.getYawVelocity());
            io[i].updateInputs(inputs[i]);
            Logger.processInputs("Vision/" + indexToName(i), inputs[i]);
        }
    }

    @Override
    public void doPeriodic() {
        List<Pose3d> allTagPoses = new LinkedList<>();
        List<Pose3d> allBotPoses = new LinkedList<>();
        List<Pose3d> allAcceptedBotPoses = new LinkedList<>();
        List<Pose3d> allRejectedBotPoses = new LinkedList<>();

        for (int cameraIndex = 0; cameraIndex < io.length; cameraIndex++) {
            List<Pose3d> tagPoses = new LinkedList<>();
            List<Pose3d> botPoses = new LinkedList<>();
            List<Pose3d> acceptedBotPoses = new LinkedList<>();
            List<Pose3d> rejectedBotPoses = new LinkedList<>();

            // add all the tag poses
            for (TargetObservation targetObservation : inputs[cameraIndex].latestTargetObservations) {
                tagPoses.add(
                    VisionConstants.TAG_LAYOUT.getTagPose(targetObservation.id()).orElse(Pose3d.kZero));
            }

            // process all of our pose observations
            for (var observation : inputs[cameraIndex].poseObservations) {
                // filter out bad poses
                boolean rejectPose =
                    // must have tags in view
                    observation.tagCount() == 0

                    // ambiguity can't be too high. Ambiguity doesn't exist for
                    // multitag readings, so it's only applicable for single tag
                    // readings
                    || (observation.tagCount() == 1
                        && observation.ambiguity() > VisionConstants.MAX_AMBIGUITY)

                    // Z coordinate must be realistic
                    || Math.abs(observation.pose().getZ()) > VisionConstants.MAX_Z_ERROR

                    // Megatag2 is unreliable when rotating, so don't use Megatag2 readings when rotating
                    || (observation.type() == PoseObservationType.MEGATAG2
                        && swerve.getYawVelocity().getDegrees() > 2)

                    // Must be within the field boundaries
                    || observation.pose().getX() <= 0.0
                    || observation.pose().getX() > VisionConstants.TAG_LAYOUT.getFieldLength()
                    || observation.pose().getY() <= 0.0
                    || observation.pose().getY() > VisionConstants.TAG_LAYOUT.getFieldWidth();

                // add pose to log
                botPoses.add(observation.pose());
                if (rejectPose) {
                    rejectedBotPoses.add(observation.pose());

                    // the rest of the computation for this loop
                    // doesn't matter if the pose was rejected
                    continue;
                } else {
                    acceptedBotPoses.add(observation.pose());
                }

                // calculate standard deviations (camera pose fusion is done
                // through a Kalman Filter, which defines a gaussian curve for
                // each sensor input)
                double distanceFactor =
                    observation.averageTagDistance()
                    * observation.averageTagDistance()
                    / observation.tagCount();
                double cameraFactor = VisionConstants.CAMERA_STD_DEV_FACTORS[cameraIndex];
                double linearStdDevs =
                    VisionConstants.LINEAR_STD_DEV_BASE
                    * distanceFactor
                    * cameraFactor;
                double angularStdDevs =
                    VisionConstants.ANGULAR_STD_DEV_BASE
                    * distanceFactor
                    * cameraFactor;

                swerve.addVisionMeasurement(
                    observation.pose().toPose2d(),
                    observation.timestamp(),
                    VecBuilder.fill(
                        linearStdDevs * observation.type().stdDevFactorX,
                        linearStdDevs * observation.type().stdDevFactorY,
                        angularStdDevs * observation.type().stdDevFactorTheta));
            }

            // Log camera data
            Logger.recordOutput(
                "Vision/" + indexToName(cameraIndex) + "/TagPoses",
                tagPoses.toArray(new Pose3d[tagPoses.size()]));

            Logger.recordOutput(
                "Vision/" + indexToName(cameraIndex) + "/BotPoses",
                botPoses.toArray(new Pose3d[botPoses.size()]));

            Logger.recordOutput(
                "Vision/" + indexToName(cameraIndex) + "/BotPosesAccepted",
                acceptedBotPoses.toArray(new Pose3d[acceptedBotPoses.size()]));

            Logger.recordOutput(
                "Vision/" + indexToName(cameraIndex) + "/BotPosesRejected",
                rejectedBotPoses.toArray(new Pose3d[rejectedBotPoses.size()]));

            allTagPoses.addAll(tagPoses);
            allBotPoses.addAll(botPoses);
            allAcceptedBotPoses.addAll(acceptedBotPoses);
            allRejectedBotPoses.addAll(rejectedBotPoses);
        }

        // Log summary data
        Logger.recordOutput(
            "Vision/Summary/TagPoses", allTagPoses.toArray(new Pose3d[allTagPoses.size()]));

        Logger.recordOutput(
            "Vision/Summary/RobotPoses", allBotPoses.toArray(new Pose3d[allBotPoses.size()]));

        Logger.recordOutput(
            "Vision/Summary/BotPosesAccepted",
            allAcceptedBotPoses.toArray(new Pose3d[allAcceptedBotPoses.size()]));

        Logger.recordOutput(
            "Vision/Summary/BotPosesRejected",
            allRejectedBotPoses.toArray(new Pose3d[allRejectedBotPoses.size()]));
    }

    public boolean frontCanSeeSpeakerTag() {
        return getSpeakerObservation().isPresent();
    }

    public Optional<Double> getSpeakerTagDistance() {
        var observation = getSpeakerObservation();
        if (observation.isPresent()) {
            return Optional.of(observation.get().distToTag());
        }
        return Optional.empty();
    }

    public Optional<Pose2d> getFrontCameraPose() {
        Pose3d ret = inputs[VisionConstants.FRONT_CAM_INDEX].poseObservations[0].pose();
        if (ret != null) {
            return Optional.of(ret.toPose2d());
        } else {
            return Optional.empty();
        }
    }

    public boolean anyCameraDisconnected() {
        for (var input : inputs) {
            if (!input.connected) return true;
        }
        return false;
    }

    private Optional<TargetObservation> getSpeakerObservation() {
        for (TargetObservation observation :
            inputs[VisionConstants.FRONT_CAM_INDEX].latestTargetObservations) {
            if (observation.id() == VisionConstants.BLUE_SPEAKER_ID
                || observation.id() == VisionConstants.RED_SPEAKER_ID) {
                return Optional.of(observation);
            }
        }
        return Optional.empty();
    }

    private String indexToName(int index) {
        return switch (index) {
            case 0 -> "limelight-front";
            case 1 -> "limelight-left";
            case 2 -> "limelight-right";
            case 3 -> "limelight-back";
            default -> "";
        };
    }

    private Transform3d indexToPosition(int index) {
        return switch (index) {
            case 0 -> VisionConstants.FRONT_CAM_POS;
            case 1 -> VisionConstants.LEFT_CAM_POS;
            case 2 -> VisionConstants.RIGHT_CAM_POS;
            case 3 -> VisionConstants.BACK_CAM_POS;
            default -> Transform3d.kZero;
        };
    }

    @Override
    public void reset() {}
    
    @Override
    public boolean getHealth() {
        for (VisionIOInputsAutoLogged cameraInputs : inputs) {
            if (!cameraInputs.connected) {
                return false;
            }
        }
        return true;
    }
}
