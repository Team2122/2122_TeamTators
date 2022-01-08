package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.DriveInputSupplier.DriveInput;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Drive;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.scheduler.Command;

import java.util.function.Supplier;

public class DriveBack extends Command implements Configurable<DriveBack.Config>, Supplier<DriveInput> {

    private Drive drive;
    private Config config;
    private DriveInput input;
    private Supplier<DriveInput> previousSupplier;

    private NEOEncoder encoder;
    private double initialRotations;
    private boolean localDebug = false;

    public DriveBack(TatorRobot robot) {
        super("DriveBack");
        drive = robot.getSubsystems().getDrive();
        input = new DriveInput(-.3, -.3);
        encoder = drive.leftTransmissionEncoder;
    }

    @Override
    public DriveInput get() {
        return input;
    }

    @Override
    public void initialize() {
        super.initialize(true);
        previousSupplier = drive.inputSupplier.getSupplier();
        drive.inputSupplier.setSupplier(this);
        initialRotations = encoder.getRotations();
        input.applyScalar(config.goBackwards ? 1 : -1);
    }

    @Override
    public boolean step() {

        // The config.goBackwards flag determines if the drive motor is configured
        // to go forwards or backwards.  Since the method is calleld Drive Back, multiplying by -1 
        // forces it to go forwards
        double rotations = (encoder.getRotations() - initialRotations) * (config.goBackwards ? 1 : -1);
        double inches = drive.rotationsToInches(rotations);

        if (localDebug) {
            logger.info("Rotations: " + rotations);
            logger.info("Inches: " + inches);
        }

        return -inches > config.distance;
    }

    @Override
    public void finish(boolean interrupted) {
        
        super.finish(interrupted, true);
        
        if ( localDebug ) {
            logger.info("Drive Back Exiting!");
        }

        drive.inputSupplier.setSupplier(previousSupplier);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public double distance;
        public boolean debug;
        public boolean goBackwards = true;
    }

}