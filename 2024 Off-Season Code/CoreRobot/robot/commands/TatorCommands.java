package frc.robot.commands;

import java.util.function.*;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.subsystems.climber.Climber;
import frc.robot.subsystems.kingRollers.KingRollers;
import frc.robot.subsystems.picker.Picker;
import frc.robot.subsystems.picker.Picker.PickerStates;
import frc.robot.subsystems.pivot.Pivot;
import frc.robot.subsystems.pivot.PivotConstants.PivotPositions;
import frc.robot.subsystems.swerve.SwerveDrive;
import frc.robot.subsystems.operatorInterface.OperatorInterface;
import frc.robot.subsystems.upperNotePath.UpperNotePath;
import frc.robot.subsystems.upperNotePath.UpperNotePath.ShotType;
import frc.robot.util.AimUtil;
import frc.robot.util.AimUtil.AimTargets;


public class TatorCommands {
    protected static Picker picker;
    protected static KingRollers kingRollers;
    protected static UpperNotePath upperNotePath;
    protected static Pivot pivot;
    protected static SwerveDrive swerve;
    protected static Climber climber;
    protected static OperatorInterface operatorInterface;

    public static void prepare(
        Picker _picker,
        KingRollers _kingRollers,
        UpperNotePath _upperNotePath,
        Pivot _pivot,
        SwerveDrive _swerve,
        Climber _climber,
        OperatorInterface _operatorInterface
    ) {
        picker = _picker;
        kingRollers = _kingRollers;
        upperNotePath = _upperNotePath;
        pivot = _pivot;
        swerve = _swerve;
        climber = _climber;
        operatorInterface = _operatorInterface;
    }

    public static Command stopAiming() {
        return swerve.drive(operatorInterface)
            .alongWith(upperNotePath.idle())
            .alongWith(pivot.goTo(PivotPositions.HOME))
            .withName("StopAiming");
    }

    public static Command stopAimingNoSwerve() {
        return upperNotePath.idle()
            .alongWith(pivot.goTo(PivotPositions.HOME))
            .withName("StopAimingNoSwerve");
    }

    public static Command specialAim() {
        return swerve.aim(operatorInterface)
            .alongWith(upperNotePath.shoot(ShotType.DYNAMIC))
            .alongWith(pivot.specialAim())
            .withName("SpecialAim");
    }

    public static Command speakerAimNoSwerve() {
        return AimUtil.speakerAim()
            .alongWith(upperNotePath.shoot(ShotType.DYNAMIC))
            .alongWith(pivot.specialAim())
            .withName("SpeakerAimNoSwerve");
    }

    public static Command pick() {
        return Commands.either(
            picker.pick(),
            Commands.none(),
            () -> picker.getState() == PickerStates.IDLE
                || picker.getState() == PickerStates.PANIC
        ).withName("Pick");
    }

    public static Command stopPicking() {
        return Commands.either(
            picker.idle(),
            Commands.none(),
            () -> picker.getState() == PickerStates.PICKING
        ).withName("Stop Pick");
    }

    public static Command ejectUpper() {
        return upperNotePath.eject()
            .andThen(kingRollers.feedUnconditionally()
                .alongWith(picker.pickUnconditionally()))
            .withName("EjectUpper");
    }

    public static Command ejectLower() {
        return pivot.goTo(PivotPositions.EJECT)
            .andThen(kingRollers.reverse()
                .alongWith(picker.panic()))
            .withName("EjectLower");
    }

    public static Command spitPieceOutPicker() {
        return kingRollers.reverse()
            .alongWith(picker.panic())
            .withName("SpitPieceOutPicker");
    }

    public static Command fullReset() {
        return upperNotePath.idle()
            .alongWith(pivot.goTo(PivotPositions.HOME))
            .alongWith(picker.idle())
            .alongWith(Commands.runOnce(() -> kingRollers.reset()))
            .withName("FullReset");
    }

    public static Command takeShot() {
        return kingRollers.feed()
            .withName("Take Shot");
    }

    public static Command startAmp() {
        return pivot.goTo(PivotPositions.AMP)
            .alongWith(
                upperNotePath.startAmp()
                .andThen(
                    upperNotePath.midAmp()
                        .deadlineFor(kingRollers.feed())
                )
                .alongWith(AimUtil.stopAim())
            ).withName("StartAmp");
    }

    public static Command endAmp() {
        return upperNotePath.endAmp()
            .andThen(pivot.goTo(PivotPositions.HOME))
            .withName("EndAmp");
    }

    public static Command prepStaticShot() {
        return pivot.goTo(PivotPositions.FIXEDSHOT)
            .alongWith(upperNotePath.shoot(ShotType.STATIC));
    }

    public static Command stopStaticShot() {
        return pivot.goTo(PivotPositions.HOME)
            .alongWith(upperNotePath.idle());
    }
}
