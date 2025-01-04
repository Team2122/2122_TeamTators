package frc.robot.commands;

import java.util.List;
import java.util.function.Supplier;

import org.littletonrobotics.junction.Logger;
import org.teamtators.Util.FlipUtil;
import org.teamtators.Util.Vector2d;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.filter.Debouncer.DebounceType;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.subsystems.operatorInterface.OperatorInterfaceConstants;
import frc.robot.subsystems.swerve.SwerveDrive;

public class DriveToPoseTarget implements Supplier<ChassisSpeeds> {
    private Pose2d target;
    private PIDController pidTransController;
    private PIDController pidRotController;
    private final double deadzone = .1;
    private final Trigger debouncer;
    private SwerveDrive swerve;

    public enum Target {
        AMP, 
        SOURCE, 
        TRAP
    }
    private Target targetType;

    public DriveToPoseTarget(DriveToPoseTarget.Target type, SwerveDrive swerve) {
        this.targetType = type;
        pidTransController = new PIDController(5, 0, 0);
        pidRotController = new PIDController(.05, 0, 0);
        debouncer = new Trigger(this::internalIsFinished).debounce(0.3, DebounceType.kRising);
        this.swerve = swerve;
    }

    // all static poses are for blue team
    private static final Pose2d staticAmpPose = new Pose2d(
        // TODO figure out optimal Y
        1.82, 7.25, Rotation2d.fromDegrees(90)
    );
    private static final Pose2d midSourcePose = new Pose2d(15.55, 0.75, Rotation2d.fromDegrees(-60.86));
    private static final List<Pose2d> trapPoses = List.of( // values from https://firstfrc.blob.core.windows.net/frc2024/FieldAssets/2024LayoutMarkingDiagram.pdf
        new Pose2d(5.320792, 4.105148, Rotation2d.fromDegrees(180)),
        new Pose2d(4.641342, 4.49834, Rotation2d.fromDegrees(-60)),
        new Pose2d(4.641342, 3.713226, Rotation2d.fromDegrees(60))
    );
    private static final double trapOffset = 0.35;//from tag to chain: 0.4191;, TODO what *should* this be

    private static final Rotation2d ANG180 = Rotation2d.fromDegrees(180);

    public void initialize() {
        Pose2d pose = swerve.getPose();
        // targetX = 0.00;
        // targetY = 0.00;
        switch (targetType) {
            case AMP:
                target = FlipUtil.conditionalFlipPose2d(staticAmpPose);
                break;
            case SOURCE:
                target = FlipUtil.conditionalFlipPose2d(midSourcePose);
                break;
            case TRAP:
                // instead of flipping all the trap poses, flip the bot pose then flip the result.
                target = FlipUtil.conditionalFlipPose2d(
                    FlipUtil.conditionalFlipPose2d(pose).nearest(trapPoses)
                );
                double angle = target.getRotation().getRadians();
                target = new Pose2d(
                    target.getX() - Math.cos(angle)*trapOffset,
                    target.getY() - Math.sin(angle)*trapOffset,
                    target.getRotation().plus(ANG180)
                );
                break;
            default:
                if (DriverStation.isFMSAttached()) {
                    System.err.println("[AutoDriveToTarget] Invalid type " + targetType.name());
                    target = pose;
                } else {
                    throw new Error("AAAAA WHAT IS THIS AUTODRIVETONODE TYPE " + targetType.name());
                }
        }
        System.out.println(target);

        try {
            Logger.recordOutput("DriveToPoseTarget/Target", target);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public ChassisSpeeds get() {
        Pose2d pose = swerve.getPose();
        double gyroOffset = Math.toDegrees(MathUtil.angleModulus(target.getRotation().getRadians() - pose.getRotation().getRadians()));

        var speeds = new ChassisSpeeds(
            pidTransController.calculate(pose.getX(), target.getX()),
            pidTransController.calculate(pose.getY(), target.getY()),
            pidRotController.calculate(0, gyroOffset)
        );
        speeds.toRobotRelativeSpeeds(pose.getRotation());

        return applyDrag(speeds);
    }

    private ChassisSpeeds applyDrag(ChassisSpeeds in) {
        var speed = TatorCommands.swerve.getChassisSpeeds();
        var dist = Math.hypot(
            speed.vxMetersPerSecond - in.vxMetersPerSecond,
            speed.vyMetersPerSecond - in.vyMetersPerSecond
        );
        if (dist > OperatorInterfaceConstants.maxAccel) {
            var offFac = dist / OperatorInterfaceConstants.maxAccel;
            in.vxMetersPerSecond = (in.vxMetersPerSecond - speed.vxMetersPerSecond) / offFac + speed.vxMetersPerSecond;
            in.vyMetersPerSecond = (in.vyMetersPerSecond - speed.vyMetersPerSecond) / offFac + speed.vyMetersPerSecond;
        }
        return in;
    }

    public boolean controllerOverride() {
        Vector2d adjTrans = TatorCommands.operatorInterface.getAdjustedTranslation();
        if (
            Math.abs(TatorCommands.operatorInterface.getRotationHorizontal()) >= deadzone
            || Math.abs(adjTrans.getX()) >= deadzone
            || Math.abs(adjTrans.getY()) >= deadzone
        ) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean atTargetPosition() {
        var currentPose = swerve.getPose();
        if (
            Math.abs(MathUtil.angleModulus(currentPose.getRotation().getRadians() - target.getRotation().getRadians())) < OperatorInterfaceConstants.angleError &&
            currentPose.getTranslation().getDistance(target.getTranslation()) < OperatorInterfaceConstants.posError
        ) {
            return true;
        }
        return false;
    }

    private boolean internalIsFinished() {
        if (atTargetPosition()) {
            System.out.println("DriveToPoseTarget to target finished due to reaching end");
            return true;
        } else if (controllerOverride()) {
            System.out.println("DriveToPoseTarget to target finished due to controller override");
            return true;
        } else {
            return false;
        }
    }

    public boolean isFinished() {
        return debouncer.getAsBoolean();
    }
}
