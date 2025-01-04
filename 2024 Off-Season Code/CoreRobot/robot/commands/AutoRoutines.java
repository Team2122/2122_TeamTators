package frc.robot.commands;

import java.io.IOException;
import java.util.Set;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.Subsystem;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.subsystems.picker.Picker;
import frc.robot.subsystems.picker.Picker.PickerStates;
import frc.robot.subsystems.kingRollers.KingRollers;
import frc.robot.subsystems.kingRollers.KingRollers.KingRollerStates;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.swerve.SwerveDrive.SwerveStates;
import frc.robot.subsystems.upperNotePath.UpperNotePath;
import frc.robot.util.AimUtil;
import frc.robot.util.AimUtil.AimTargets;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.event.EventLoop;

import com.pathplanner.lib.auto.*;
import com.pathplanner.lib.path.*;
import com.pathplanner.lib.util.FileVersionException;
import com.pathplanner.lib.util.FlippingUtil;
import com.pathplanner.lib.util.GeometryUtil;

import org.json.simple.parser.ParseException;

public class AutoRoutines {
    public enum BranchDirection {
        UP,
        DOWN
    }

    private static SwerveDrive swerve;

    private enum ShotState {
        UNDEFINED,
        EMPTY,
        EMPTY_START,
        READY
    }

    public static final EventLoop eventLoop = new EventLoop();

    public static void prepare(
        SwerveDrive _swerve,
        Picker picker,
        UpperNotePath upperNotePath,
        KingRollers kingRollers)
    {
        swerve = _swerve;

        NamedCommands.registerCommand("Pick", TatorCommands.pick());
        NamedCommands.registerCommand("Shoot", TatorCommands.takeShot()
            .andThen(TatorCommands.stopAimingNoSwerve()
                .alongWith(Commands.runOnce(() -> swerve.setState(SwerveStates.DRIVE)))));
        NamedCommands.registerCommand("ShootKeepAiming", TatorCommands.takeShot());
        NamedCommands.registerCommand("Shoot Preload", TatorCommands.speakerAimNoSwerve()
            .andThen(Commands.waitSeconds(0.5))
            .andThen(TatorCommands.takeShot()));
        NamedCommands.registerCommand("DropPreload", TatorCommands.spitPieceOutPicker()
            .until(() -> !picker.getFarSensor())
            .withTimeout(2)
            .andThen(TatorCommands.pick()));
        NamedCommands.registerCommand("StartAiming", TatorCommands.speakerAimNoSwerve()
            .alongWith(Commands.runOnce(() -> swerve.setState(SwerveStates.SPECIAL_AIMING))));
        NamedCommands.registerCommand("TakeFinalShot", Commands.waitSeconds(.15)
            .andThen(Commands.waitUntil(() -> kingRollers.noteSensed() && kingRollers.getState() == KingRollerStates.READY))
            .andThen(TatorCommands.takeShot()));
    }

    public static Command closeThree() throws IOException, ParseException, FileVersionException {
        PathPlannerPath path = PathPlannerPath.fromChoreoTrajectory("close_three");
        return resetPose(path.getStartingHolonomicPose().orElse(Pose2d.kZero))
            .andThen(AutoBuilder.followPath(path));
    }

    private static Command resetPose(Pose2d pose) {
        return Commands.runOnce(() -> {
            // HACKY SOLUTION TO GET AROUND THE POSE ROTATION BEING WEIRD
            Pose2d startPose = new Pose2d(pose.getTranslation(), Rotation2d.fromDegrees(180));
            if (Robot.getAlliance().equals(Alliance.Red)) {
                startPose = FlippingUtil.flipFieldPose(startPose);
            }

            swerve.resetPose(startPose);
        });
    }
}
