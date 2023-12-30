package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.RobotContainer;
import frc.robot.constants.PinkarmConstants.ArmExtensionConstants;
import frc.robot.constants.WristConstants.WristPositions;
import frc.robot.subsystems.ArmExtension;
import frc.robot.subsystems.ArmRotation;
import frc.robot.subsystems.Claw;
import frc.robot.subsystems.OperatorInterface;
import frc.robot.subsystems.SwerveDrive;
import frc.robot.subsystems.Wrist;
import frc.robot.subsystems.ArmExtension.ExtensionPosition;
import frc.robot.subsystems.ArmRotation.RotationPosition;
import frc.robot.RobotContainer.GamePieceTypes;
import frc.robot.RobotContainer.PlacePositions;

import static frc.robot.RobotContainer.GamePieceTypes.*;

import java.util.Optional;

public class CommandFactory {
    ArmRotation armRotation;
    ArmExtension armExtension;
    Wrist wrist;
    Claw claw;
    SwerveDrive swerveDrive;
    OperatorInterface operatorInterface;

    public CommandFactory(RobotContainer robotContainer) {
        this.armRotation = robotContainer.getArmRotation();
        this.armExtension = robotContainer.getArmExtension();
        this.wrist = robotContainer.getWrist();
        this.claw = robotContainer.getClaw();
        this.swerveDrive = robotContainer.getSwerveDrive();
        this.operatorInterface = robotContainer.getOperatorInterface();
    }

    public Command shelfPick(GamePieceTypes piece) {
        return Commands.parallel(
            Commands.print("Shelf Pick " + piece + " Command Scheduled"),
            wrist.toPosition(WristPositions.SHELF_PICK),
            armRotation.toPosition(RotationPosition.HIGH_PICK)
                .andThen(armExtension.toPosition(ExtensionPosition.HIGH_PICK))
                .andThen(claw.suck(piece))
        );
    }

    public Command floorPick(GamePieceTypes piece) {
        Command ret;

        Command armToPosition = Commands.sequence(
            armRotation.toPosition(piece.equals(CONE)
                ? RotationPosition.FLOOR_PICK_CONE
                : RotationPosition.FLOOR_PICK_CUBE),
            armExtension.toPosition(piece.equals(CONE)
                ? ExtensionPosition.FLOOR_PICK_CONE
                : ExtensionPosition.FLOOR_PICK_CUBE)
        );

        if(armExtension.getCurrentPosition().getInches() < ArmExtensionConstants.idleInches + ArmExtensionConstants.error) {
            ret = Commands.print("Floor Pick " + piece + " Command Scheduled")
                .andThen(wrist.toPosition(WristPositions.FLOOR_PICK)
                    .alongWith(armToPosition))
                .andThen(claw.suck(piece));
        } else {
            ret = Commands.print("Floor pick scheduled while still extended");
        }

        return ret;
    }

    public Command goToTransport() {
        Command armToPosition = Commands.sequence(
            armExtension.toPosition(ExtensionPosition.HOME),
            armRotation.toPosition(RotationPosition.HOME)
        );

        Command wristAndArmTogether = wrist.toPosition(WristPositions.TRANSPORT)
            .alongWith(armToPosition);

        return Commands.print("Go To Transport Command Scheduled")
            .andThen(claw.stop())
            .andThen(wristAndArmTogether);
    }

    public Command place(PlacePositions position) {
        Command armToPosition = Commands.sequence(
            armRotation.toNode(position),
            armExtension.toNode(position)
        );

        return Commands.print(position + " Command Scheduled")
            .alongWith(wrist.toNode(position))
            .alongWith(armToPosition);
    }
}
