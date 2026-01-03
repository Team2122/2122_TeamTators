package org.teamtators.tester.components;

import edu.wpi.first.math.kinematics.ChassisSpeeds;
import frc.robot.subsystems.operatorInterface.OperatorInterface;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;
import frc.robot.subsystems.swerve.SwerveDrive;
import org.teamtators.tester.ManualTest;
import org.teamtators.util.QuickDebug;
import org.teamtators.util.XBOXController;

public class SwerveTuningTest extends ManualTest {
    OperatorInterface opInt;
    SwerveDrive swerve;
    boolean swerveEnabled = false;
    boolean snapToX = false;
    boolean tuningAzimuth = false;

    public SwerveTuningTest(String name, OperatorInterface opInt, SwerveDrive swerve) {
        super(name);
        this.opInt = opInt;
        this.swerve = swerve;
    }

    @Override
    public void start() {
        swerveEnabled = false;
        printTestInstructions(
                "Drive as you normally would; Use Shuffleboard to change P/D/S/V values; A to enable, B to disable, X to push Shuffleboard values to motors, Back to swap what's being tuned, and Start to gyro reset");
        // swerve.testing_setPDSV(
        //     QuickDebug.input("SwerveTuning/P", 0),
        //     QuickDebug.input("SwerveTuning/D", 0),
        //     QuickDebug.input("SwerveTuning/S", 0),
        //     QuickDebug.input("SwerveTuning/V", 0)
        // );
    }

    @Override
    public void update() {
        var chassis = opInt.get();
        if (snapToX && !tuningAzimuth) chassis.vyMetersPerSecond = 0;
        swerve.setVelocitySetpoint(chassis);
        if (swerveEnabled) {
            swerve.doPeriodic();
        }
        QuickDebug.output(
                "SwerveTuning/DesiredSpeed",
                Math.hypot(chassis.vxMetersPerSecond, chassis.vyMetersPerSecond));
        QuickDebug.output("SwerveTuning/DesiredSpin", chassis.omegaRadiansPerSecond);
        var actualChassis = swerve.getChassisSpeeds();
        QuickDebug.output(
                "SwerveTuning/ActualSpeed",
                Math.hypot(actualChassis.vxMetersPerSecond, actualChassis.vyMetersPerSecond));
        QuickDebug.output("SwerveTuning/ActualSpin", actualChassis.omegaRadiansPerSecond);
    }

    private void freeze() {
        swerveEnabled = false;
        swerve.setVelocitySetpoint(new ChassisSpeeds());
        swerve.doPeriodic();
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        switch (button) {
            case kA:
                swerveEnabled = true;
                printTestInfo("System enabled");
                break;
            case kB:
                freeze();
                printTestInfo("System disabled");
                break;
            case kX:
                freeze();
                printTestInfo(
                        "System disabled, pushing values to " + (tuningAzimuth ? "azimuth" : "drive"));
                swerve.testing_setPDSV(
                        QuickDebug.input("SwerveTuning/P", 0),
                        QuickDebug.input("SwerveTuning/D", 0),
                        QuickDebug.input("SwerveTuning/S", 0),
                        QuickDebug.input("SwerveTuning/V", 0),
                        (tuningAzimuth ? SwerveModuleMotor.AZIMUTH : SwerveModuleMotor.DRIVE));
                break;
            case kY:
                freeze();
                snapToX = !snapToX;
                printTestInfo("System disabled, X snap mode set to " + snapToX);
                break;
            case kBACK:
                freeze();
                tuningAzimuth = !tuningAzimuth;
                printTestInfo("System disabled, now tuning " + (tuningAzimuth ? "azimuth" : "drive"));
                break;
            case kSTART:
                swerve.resetPoseRotation();
                printTestInfo("Gyro reset");
                break;
            default:
                this.printTestInfo("unknown button action for swerve tuning: " + button.name());
                break;
        }
    }

    // TODO add end
}
