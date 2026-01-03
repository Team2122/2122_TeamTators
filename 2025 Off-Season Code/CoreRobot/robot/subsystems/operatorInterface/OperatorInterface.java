package frc.robot.subsystems.operatorInterface;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Robot;
import frc.robot.Robot.RobotControlMode;
import frc.robot.subsystems.swerve.SwerveDrive;
import java.util.function.Supplier;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;
import org.teamtators.util.FlipUtil;
import org.teamtators.util.JoystickModifiers;
import org.teamtators.util.Subsystem;
import org.teamtators.util.Vector2d;

public class OperatorInterface extends Subsystem implements Supplier<ChassisSpeeds> {
    private CommandXboxController controller;

    private JoystickModifiers translationModifiers;
    private JoystickModifiers rotationModifiers;
    private SwerveDrive swerve;
    private ChassisSpeeds output = new ChassisSpeeds();

    public final Trigger nonzeroInput;

    public OperatorInterface() {
        super();
        setValidity(RobotControlMode.Disabled, RobotControlMode.Test, RobotControlMode.Teleop);
        translationModifiers =
                new JoystickModifiers(
                        OperatorInterfaceConstants.offset,
                        OperatorInterfaceConstants.deadzone,
                        OperatorInterfaceConstants.translationExponent,
                        OperatorInterfaceConstants.translationCutOffX,
                        OperatorInterfaceConstants.translationCutOffY);
        rotationModifiers =
                new JoystickModifiers(
                        OperatorInterfaceConstants.offset,
                        OperatorInterfaceConstants.deadzone,
                        OperatorInterfaceConstants.rotationExponent,
                        OperatorInterfaceConstants.rotationCutOffX,
                        OperatorInterfaceConstants.rotationCutOffY,
                        OperatorInterfaceConstants.rotationLimitY);

        nonzeroInput =
                new Trigger(
                        () -> {
                            var schmoove = getAdjustedTranslation();
                            var spiiin = getRotationHorizontal();
                            return Math.abs(schmoove.getX()) > 0.01
                                    || Math.abs(schmoove.getY()) > 0.01
                                    || Math.abs(spiiin) > 0.01;
                        });
    }

    @Override
    public void configure() {
        this.swerve = Robot.getInstance().swerve;
        this.controller = Robot.getInstance().driverController;
    }

    /** Return the left joystick value and apply drive modifiers */
    public double getRotationHorizontal() {
        double returnVal =
                rotationModifiers.applyPiecewise(
                        controller.getRightX()
                        // QuickDebug.input("rot", 0.0) // Use this for inputting exact values
                        );
        return returnVal;
    }

    public Vector2d getAdjustedTranslation() {
        return translationModifiers.radialAdjust(
                controller.getLeftX(), controller.getLeftY()
                // QuickDebug.input("x", 0.0), // Use these for inputting exact values
                // QuickDebug.input("y", 0.0)
                );
    }

    @Override
    public void doPeriodic() {
        output = calculateOutput();
        Logger.recordOutput("OperatorInterface/NonzeroInput", nonzeroInput);
    }

    @Override
    @AutoLogOutput
    public ChassisSpeeds get() {
        return output;
    }

    private ChassisSpeeds calculateOutput() {

        Vector2d adjustedTranslation = getAdjustedTranslation();

        var base =
                new ChassisSpeeds(
                        -adjustedTranslation.getY() * OperatorInterfaceConstants.maxYDot,
                        -adjustedTranslation.getX() * OperatorInterfaceConstants.maxXDot,
                        -getRotationHorizontal() * OperatorInterfaceConstants.maxThetaDot);

        var speeds =
                ChassisSpeeds.fromFieldRelativeSpeeds(
                        base, FlipUtil.conditionalFlip(swerve.getPose().getRotation()));
        applyDrag(speeds);
        return speeds;
    }

    private void applyDrag(ChassisSpeeds in) {
        var speed = swerve.getChassisSpeeds();
        if (speed == null) return;
        var dist =
                Math.hypot(
                        speed.vxMetersPerSecond - in.vxMetersPerSecond,
                        speed.vyMetersPerSecond - in.vyMetersPerSecond);
        if (dist > getDesiredAcceleration()) {
            var offFac = dist / getDesiredAcceleration();
            in.vxMetersPerSecond =
                    (in.vxMetersPerSecond - speed.vxMetersPerSecond) / offFac + speed.vxMetersPerSecond;
            in.vyMetersPerSecond =
                    (in.vyMetersPerSecond - speed.vyMetersPerSecond) / offFac + speed.vyMetersPerSecond;
        }
    }

    private double getDesiredAcceleration() {
        return OperatorInterfaceConstants.maxAccel;
    }

    @Override
    public void log() {}

    @Override
    public boolean getHealth() {
        return true;
    }
}
