package org.teamtators.tester.components;

import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.hardware.TalonFX;
import org.teamtators.tester.ManualTest;

public class SwerveModuleTest extends ManualTest {
    TalonFX drive;
    TalonFX azimuth;

    DutyCycleOut driveRequest;
    DutyCycleOut azimuthRequest;

    double rightAxisValue;
    double leftAxisValue;
    double speedLimit;
    double rotationLimit;
    double driveConversion;
    double rotationConversion;

    public SwerveModuleTest(String name, TalonFX drive, TalonFX azimuth) {
        this(name, drive, azimuth, 1, 1);
    }

    public SwerveModuleTest(
            String name,
            TalonFX drive,
            TalonFX azimuth,
            double driveConversion,
            double rotationConversion) {
        this(
                name,
                drive,
                azimuth,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                driveConversion,
                rotationConversion);
    }

    public SwerveModuleTest(
            String name,
            TalonFX drive,
            TalonFX azimuth,
            double speedLimit,
            double rotationLimit,
            double driveConversion,
            double rotationConversion) {
        super(name);
        this.drive = drive;
        this.azimuth = azimuth;
        this.speedLimit = speedLimit;
        this.rotationLimit = rotationLimit;
        this.driveConversion = driveConversion;
        this.rotationConversion = rotationConversion;
        this.driveRequest = new DutyCycleOut(0);
        this.azimuthRequest = new DutyCycleOut(0);
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
    public void update() {
        double driveDutyCycle = getDriveSpeed(leftAxisValue * driveConversion);
        double azimuthDutyCycle = getDriveSpeed(rightAxisValue * rotationConversion);
        drive.setControl(driveRequest.withOutput(driveDutyCycle));
        azimuth.setControl(azimuthRequest.withOutput(azimuthDutyCycle));
    }

    @Override
    public void stop() {
        drive.stopMotor();
        azimuth.stopMotor();
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
