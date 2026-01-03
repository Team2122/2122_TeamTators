package frc.robot.subsystems.vision;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.filter.Debouncer;
import edu.wpi.first.math.geometry.Pose3d;
import frc.robot.Robot;
import frc.robot.Robot.RobotControlMode;
import frc.robot.constants.Constants;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.vision.VisionConstants.PoseObservationType;
import frc.robot.subsystems.vision.VisionConstants.TargetObservation;
import java.util.LinkedList;
import java.util.List;
import org.littletonrobotics.junction.Logger;
import org.teamtators.util.Subsystem;

public class Vision extends Subsystem {
    private VisionIO[] io;
    private VisionIOInputsAutoLogged[] inputs;
    private SwerveDrive swerve;
    private Debouncer gyroZeroed = new Debouncer(.4);

    public enum VisionState {
        LEFT_CAM,
        RIGHT_CAM,
        TRINOCULAR,
        BLIND,
    }

    private VisionState state = VisionState.TRINOCULAR;

    public Vision() {
        io = new VisionIO[2];
        inputs = new VisionIOInputsAutoLogged[io.length];
        for (int i = 0; i < io.length; i++) {
            io[i] =
                    switch (Constants.kRobotMedium) {
                        case REAL -> new VisionIOLimelight(indexToName(i));
                        default -> new VisionIO() {};
                    };
            inputs[i] = new VisionIOInputsAutoLogged();
        }

        setValidity(RobotControlMode.Autonomous, RobotControlMode.Teleop, RobotControlMode.Disabled);
    }

    @Override
    public void configure() {
        this.swerve = Robot.getInstance().swerve;
    }

    @Override
    public void log() {
        for (int i = 0; i < io.length; i++) {
            io[i].setOrientation(swerve.getPose().getRotation(), swerve.getYawVelocity());
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

            // adds all the tag poses
            boolean badTag = false;
            for (TargetObservation targetObservation : inputs[cameraIndex].latestTargetObservations) {
                // 12 & 2 have bad glare in the shed, don't want good lighting
                // messing us up at comp
                if (targetObservation.id() == 12 || targetObservation.id() == 2) {
                    badTag = true;
                    continue;
                }
                tagPoses.add(
                        VisionConstants.TAG_LAYOUT.getTagPose(targetObservation.id()).orElse(Pose3d.kZero));
            }

            // this processes all of our pose observations
            for (var observation : inputs[cameraIndex].poseObservations) {
                // filter out bad poses
                boolean rejectPose =
                        // the tags have to be in view
                        observation.tagCount() == 0

                                // ambiguity can't be too high. Ambiguity doesn't exist for
                                // multitag readings, so it's only applicable for single tag
                                // readings
                                || (observation.tagCount() == 1
                                        && observation.ambiguity() > VisionConstants.MAX_AMBIGUITY)

                                // Z coordinate must be possible/reasonable
                                || Math.abs(observation.pose().getZ()) > VisionConstants.MAX_Z_ERROR

                                // Megatag2 is unreliable when rotating, so don't use Megatag2 readings when
                                // rotating
                                || (observation.type() == PoseObservationType.MEGATAG2
                                        && swerve.getYawVelocity().getDegrees() > 2)

                                // Must be within the field boundaries
                                || observation.pose().getX() <= 0.0
                                || observation.pose().getX() > VisionConstants.TAG_LAYOUT.getFieldLength()
                                || observation.pose().getY() <= 0.0
                                || observation.pose().getY() > VisionConstants.TAG_LAYOUT.getFieldWidth()

                                // Don't accept Megatag2 until the gyro is zeroed
                                // Also give the network some time to make sure the limelight sees the zeroed gyro
                                || (observation.type() == PoseObservationType.MEGATAG2
                                        && !gyroZeroed.calculate(swerve.poseCalibrated()))
                                || badTag

                                // Ability to close one eye for tag lineup
                                || (indexToName(cameraIndex).equals(VisionConstants.LEFT_CAM_NAME)
                                        && (state == VisionState.RIGHT_CAM || state == VisionState.BLIND))
                                || (indexToName(cameraIndex).equals(VisionConstants.RIGHT_CAM_NAME)
                                        && (state == VisionState.LEFT_CAM || state == VisionState.BLIND));

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
                double linearStdDevs = VisionConstants.LINEAR_STD_DEV_BASE * distanceFactor * cameraFactor;
                double angularStdDevs =
                        VisionConstants.ANGULAR_STD_DEV_BASE * distanceFactor * cameraFactor;

                swerve.addVisionMeasurement(
                        observation.pose().toPose2d(),
                        observation.timestamp(),
                        VecBuilder.fill(
                                linearStdDevs * observation.type().stdDevFactorX,
                                linearStdDevs * observation.type().stdDevFactorY,
                                angularStdDevs * observation.type().stdDevFactorTheta));
            }

            // Log the camera data
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

        Logger.recordOutput("Vision/State", state);
    }

    public boolean anyCameraDisconnected() {
        for (var input : inputs) {
            if (!input.connected) {
                return true;
            }
        }
        return false;
    }

    private String indexToName(int index) {
        return switch (index) {
            case 0 -> VisionConstants.LEFT_CAM_NAME;
            case 1 -> VisionConstants.RIGHT_CAM_NAME;
            default -> "";
        };
    }

    public void setState(VisionState state) {
        this.state = state;
    }

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
