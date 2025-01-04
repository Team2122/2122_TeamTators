package frc.robot.subsystems.operatorInterface;

import java.util.function.Supplier;

import org.teamtators.Util.JoystickModifiers;
import org.teamtators.Util.Vector2d;
import org.teamtators.Util.Subsystem;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.swerve.SwerveDrive;

public class OperatorInterface extends Subsystem implements Supplier<ChassisSpeeds> {
    private CommandXboxController controller;

    private JoystickModifiers translationModifiers;
    private JoystickModifiers rotationModifiers;
    private SwerveDrive swerve;

    public OperatorInterface(CommandXboxController controller,
        SwerveDrive swerve)
    {
        super();
        this.controller = controller;
        translationModifiers = new JoystickModifiers(
            OperatorInterfaceConstants.offset, 
            OperatorInterfaceConstants.deadzone, 
            OperatorInterfaceConstants.translationExponent, 
            OperatorInterfaceConstants.translationCutOffX, 
            OperatorInterfaceConstants.translationCutOffY
        );
        rotationModifiers = new JoystickModifiers(
            OperatorInterfaceConstants.offset, 
            OperatorInterfaceConstants.deadzone, 
            OperatorInterfaceConstants.rotationExponent, 
            OperatorInterfaceConstants.rotationCutOffX, 
            OperatorInterfaceConstants.rotationCutOffY,
            OperatorInterfaceConstants.rotationLimitY
        );

        this.swerve = swerve;
    }

    /**
     * Return the left joystick value and apply drive modifiers
     */
    public double getRotationHorizontal() {
        double returnVal = rotationModifiers.applyPiecewise(
            controller.getRightX()
            // QuickDebug.input("rot", 0.0) // Use this for inputting exact values
        );
        return returnVal ;
    }

    public Vector2d getAdjustedTranslation() {
        return translationModifiers.radialAdjust(
            controller.getLeftX(),
            controller.getLeftY()
            // QuickDebug.input("x", 0.0), // Use these for inputting exact values
            // QuickDebug.input("y", 0.0)
        );
    }

    @Override
    public ChassisSpeeds get() {

        Vector2d adjustedTranslation = getAdjustedTranslation();

        var speeds = new ChassisSpeeds(
            -adjustedTranslation.getY() * OperatorInterfaceConstants.maxYDot,
            -adjustedTranslation.getX() * OperatorInterfaceConstants.maxXDot,
            -getRotationHorizontal() * OperatorInterfaceConstants.maxThetaDot
        );
        speeds.toRobotRelativeSpeeds(swerve.getRotation2d());
        applyDrag(speeds);
        return speeds;
    }

    private void applyDrag(ChassisSpeeds in) {
        var speed = swerve.getChassisSpeeds();
        var dist = Math.hypot(
            speed.vxMetersPerSecond - in.vxMetersPerSecond,
            speed.vyMetersPerSecond - in.vyMetersPerSecond
        );
        if (dist > OperatorInterfaceConstants.maxAccel) {
            var offFac = dist / OperatorInterfaceConstants.maxAccel;
            in.vxMetersPerSecond = (in.vxMetersPerSecond - speed.vxMetersPerSecond) / offFac + speed.vxMetersPerSecond;
            in.vyMetersPerSecond = (in.vyMetersPerSecond - speed.vyMetersPerSecond) / offFac + speed.vyMetersPerSecond;
        }
    }

    @Override public void log() {}
    @Override public void reset() {}
    @Override public boolean getHealth() { return true; }

}
