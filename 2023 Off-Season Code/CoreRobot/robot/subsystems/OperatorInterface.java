package frc.robot.subsystems;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;

import com.google.common.base.Supplier;
import com.revrobotics.CANSparkMaxLowLevel;
import edu.wpi.first.wpilibj.motorcontrol.PWMSparkMax;
import edu.wpi.first.wpilibj.motorcontrol.PWMVictorSPX;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.RobotContainer;
import frc.robot.constants.OperatorInterfaceConstants;
import main.Vector2D;
import common.Controllers.XBOXController;
import common.Controllers.XBOXController.Axis;
import common.Tools.tester.ManualTestGroup;
import common.Util.JoystickModifiers;
import common.Util.*;
import common.teamtators.Subsystem;

import edu.wpi.first.wpilibj.PneumaticsControlModule;

public class OperatorInterface extends Subsystem implements Supplier<ChassisSpeeds> {

//public class OperatorInterface extends Subsystem implements ManualTestable, SwerveInputSupplier {



    private XBOXController driverController;
    private XBOXController gunnerController;
    private JoystickModifiers translationJoystickModifiers;
    private JoystickModifiers rotationJoystickModifiers;
    private TatorPigeon swerveGyro;


    public OperatorInterface(RobotContainer robotContainer) {
        super(robotContainer);
        driverController = robotContainer.getDriverController();
        gunnerController = robotContainer.getGunnerController();
        translationJoystickModifiers = new JoystickModifiers(
            OperatorInterfaceConstants.offset, 
            OperatorInterfaceConstants.deadzone, 
            OperatorInterfaceConstants.translationExponent, 
            OperatorInterfaceConstants.translationCutOffX, 
            OperatorInterfaceConstants.translationCutOffY
        );
        rotationJoystickModifiers = new JoystickModifiers(
            OperatorInterfaceConstants.offset, 
            OperatorInterfaceConstants.deadzone, 
            OperatorInterfaceConstants.rotationExponent, 
            OperatorInterfaceConstants.rotationCutOffX, 
            OperatorInterfaceConstants.rotationCutOffY
        );
        this.swerveGyro = robotContainer.getSwerveDrive().getGyro();

        // SmartDashboard.putData("cutoffthing", new SendableTranslationCutoffs());
    }

    private class SendableTranslationCutoffs implements Sendable {
        @Override
        public void initSendable(SendableBuilder builder) {
          builder.setSmartDashboardType("TheCutoffThing");
          builder.addDoubleProperty("x", this::getX, this::setX);
          builder.addDoubleProperty("y", this::getY, this::setY);
        }

        public double getX() {
            return OperatorInterfaceConstants.translationCutOffX;
        }

        public double getY() {
            return OperatorInterfaceConstants.translationCutOffY;
        }

        public void setX(double n) {
            OperatorInterfaceConstants.translationCutOffX = n;
        }

        public void setY(double n) {
            OperatorInterfaceConstants.translationCutOffY = n;
        }
    }

    

    // public double getLeftHorizontal() { // Return the left joystick value and apply drive modifiers
    //     return radialAdjust(driverController, JoystickSide.LEFT, JoystickAxisType.HORIZ);
    // }

    // public double getLeftVertical() { // Return the left joystick value and apply drive modifiers
    //     return -radialAdjust(driverController, JoystickSide.LEFT, JoystickAxisType.VERT);
    // }

    // This code is never used, so not sure why we still have it here
    // Maybe from the old tank drive days
    // public double getRotationVertical() {
    //     double returnVal = modifyJoystickInput(-driverController.getAxisValue(Axis.kRIGHT_STICK_Y), OperatorInterfaceConstants.cutoffRotationX, OperatorInterfaceConstants.cutoffRotationY);
    //     return returnVal ;
    // }

    // public Vector getTranslationHorizontal() {
    //
    //     Vector returnVal = new Vector( joystickModifiers.apply(driverController.getAxisValue(Axis.kLEFT_STICK_X)),
    //                                    joystickModifiers.apply(-driverController.getAxisValue(Axis.kLEFT_STICK_Y)));
    //
    //     return returnVal ;
    // }

    // public Vector getTranslationVertical() {
    //
    //     Vector returnVal = new Vector( joystickModifiers.apply(driverController.getAxisValue(Axis.kRIGHT_STICK_X)),
    //                                    joystickModifiers.apply(-driverController.getAxisValue(Axis.kRIGHT_STICK_Y)));
    //
    //     return returnVal ;                                       
    // }

    /**
     * Return the left joystick value and apply drive modifiers
     */
    public double getRotationHorizontal() {
        double returnVal = rotationJoystickModifiers.applyPiecewise(
            driverController.getAxisValue(Axis.kRIGHT_STICK_X)
        );
        return returnVal ;
    }

    @Override
    public void reset() {}

    public Vector2D getAdjustedTranslation() {
        return translationJoystickModifiers.radialAdjust(
            driverController.getAxisValue(Axis.kLEFT_STICK_X),
            driverController.getAxisValue(Axis.kLEFT_STICK_Y)
        );
    }

    @Override
    public ChassisSpeeds get() {

        Vector2D adjustedTranslation = getAdjustedTranslation();

        return ChassisSpeeds.fromFieldRelativeSpeeds(
            -adjustedTranslation.getX() * OperatorInterfaceConstants.maxYDot,
            adjustedTranslation.getY() * OperatorInterfaceConstants.maxXDot,
            -getRotationHorizontal() * OperatorInterfaceConstants.maxThetaDot,
            Rotation2d.fromDegrees(Math.toDegrees( swerveGyro.getYawContinuous()))
        );
    }
}
