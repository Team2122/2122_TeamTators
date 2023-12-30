package common.Tools.tester.components;

import com.ctre.phoenix6.controls.VelocityDutyCycle;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.subsystems.SwerveModule;

public class EightMotorSwerveModuleTest extends ManualTest {
    SwerveModule module;
    double rightAxisValue;
    double leftAxisValue;
    double speedLimit;
    double rotationLimit;
    double driveConversion;
    double rotationConversion;

    public EightMotorSwerveModuleTest(String name, SwerveModule module, double driveConversion, double rotationConversion) {
        super(name);
        this.module = module;
        speedLimit = Double.POSITIVE_INFINITY;
        this.driveConversion = driveConversion;
        this.rotationConversion = rotationConversion;
    }

    public EightMotorSwerveModuleTest(String name, SwerveModule module, double speedLimit, double rotationLimit, double driveConversion, double rotationConversion) {
        super(name);
        this.module = module;
        this.speedLimit = speedLimit;
        this.rotationLimit = rotationLimit;
        this.driveConversion = driveConversion;
        this.rotationConversion = rotationConversion;
    }

    @Override
    public void start() {
        printTestInstructions("Push right joystick to rotate, Left joystick to drive");
        rightAxisValue = 0;
        leftAxisValue = 0;
    }

    private double getDriveSpeed(double speed) {
        speed = (speed > speedLimit ? speedLimit : speed);
        speed = (speed < -speedLimit ? -speedLimit : speed);
        return speed;
    }

    @Override
    public void update(double delta) {
        module.leaderFalcon.setControl(new VelocityDutyCycle(getDriveSpeed(leftAxisValue * driveConversion)));
        module.azimuth.set(getDriveSpeed(rightAxisValue * rotationConversion));
    }

    @Override
    public void stop() {
        module.leaderFalcon.stopMotor();
        module.followerFalcon.stopMotor();
        module.azimuth.stopMotor();
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
        }
    }

    @Override
    public void updateRightAxis(double value) {
        rightAxisValue = value;
    }

    @Override
    public void updateLeftAxis(double value) {
        leftAxisValue = value;
    }
}
