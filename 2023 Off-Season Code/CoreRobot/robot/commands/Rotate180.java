package frc.robot.commands;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.constants.OperatorInterfaceConstants;
import frc.robot.subsystems.OperatorInterface;
import frc.robot.subsystems.SwerveDrive;
import main.Vector2D;

public class Rotate180 extends CommandBase {
    private SwerveDrive swerveDrive;
    private double initialGyroAngle;
    private OperatorInterface operatorInterface;
    private PIDController pidController;

    public Rotate180(SwerveDrive swerveDrive, OperatorInterface operatorInterface) {
        this.swerveDrive = swerveDrive;
        this.operatorInterface = operatorInterface;
        pidController = new PIDController(.5/Math.PI, 0, 0);
        addRequirements(swerveDrive, operatorInterface);
    }


    @Override
    public void initialize() {
        initialGyroAngle = swerveDrive.getGyro().getYawContinuous();
    }

    @Override
    public void execute() {
        Vector2D adjustedTranslation = operatorInterface.getAdjustedTranslation();

            swerveDrive.accept(ChassisSpeeds.fromFieldRelativeSpeeds(
                adjustedTranslation.getY() * OperatorInterfaceConstants.maxYDot,
                adjustedTranslation.getX() * OperatorInterfaceConstants.maxXDot,
                pidController.calculate(swerveDrive.getGyro().getYawContinuous(), initialGyroAngle+Math.PI),
                Rotation2d.fromDegrees(Math.toDegrees( swerveDrive.getGyro().getYawContinuous()))
             ));
     }

    @Override
    public boolean isFinished() {
        if (Math.abs(operatorInterface.getRotationHorizontal()) >= OperatorInterfaceConstants.deadzone || Math.abs(swerveDrive.getGyro().getYawContinuous()-initialGyroAngle) >= Math.PI) {
            return true;
        }
        else {
            return false;
        }
    }
}
