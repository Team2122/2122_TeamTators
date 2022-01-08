package org.teamtators.bbt8r.action_commands;

import com.kauailabs.navx.frc.AHRS;
import org.teamtators.bbt8r.DriveInputSupplier;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Drive;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

import java.util.function.Supplier;

public class RotateRobot extends Command implements Configurable<RotateRobot.Config>, Supplier<DriveInputSupplier.DriveInput> {

    private Config config;
    private Drive drive;
    private DriveInputSupplier.DriveInput driveInput;
    private Supplier<DriveInputSupplier.DriveInput> previousSupplier;
    private AHRS gyro;
    private double initialAngle;
    private double desiredAngle;

    public RotateRobot(TatorRobot robot) {
        super("RotateRobot");
        drive = robot.getSubsystems().getDrive();
        gyro = drive.getGyro();
        driveInput = new DriveInputSupplier.DriveInput(0, 0);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (config.angle > 0) {
            driveInput.set(-.20, .20);
        } else {
            driveInput.set(.20, -.20);
        }

//        initialAngle = gyro.getYaw();
//        desiredAngle = (initialAngle + config.angle) % 360;
//        desiredAngle = desiredAngle > 0 ? desiredAngle : 360 - desiredAngle;
        initialAngle = 0;
        gyro.zeroYaw();
        desiredAngle = config.angle;

        previousSupplier = drive.inputSupplier.getSupplier();
        drive.inputSupplier.setSupplier(this);
    }

    @Override
    public boolean step() {
        System.out.println("Current Angle: " + gyro.getYaw() + "\tAngle Remaining: " + Math.abs(gyro.getYaw() - desiredAngle));
        return Math.abs(gyro.getYaw() - desiredAngle) < config.error;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted);
        drive.inputSupplier.setSupplier(previousSupplier);
    }

    @Override
    public DriveInputSupplier.DriveInput get() {
        return driveInput;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double angle;
        public double error = 5;
    }

}