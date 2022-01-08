package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

// put in commandRegistrar.java...
// commandStore.registerCommand("PickHomingSensor", () -> new PickHomingSensor(robot));

// if this command is good, add to the Commands.yaml...

public class PickHomingSensor extends Command implements Configurable<PickHomingSensor.Config> {

    private GroundPicker groundPicker;
    private Config config;
    private double retractMotorPower;
    private boolean localDebug = false;

    public PickHomingSensor(TatorRobot robot) {
        super("PickHomingSensor");
        this.groundPicker = robot.getSubsystems().getGroundPicker();
    }

    public void initialize() {
        super.initialize(localDebug);
        // groundPicker.setHoming(true);
        groundPicker.setPickerAction(GroundPicker.PickerAction.HOMING);
        if (localDebug) {
            logger.info("Starting Picker Home");
        }
    }

    @Override
    public boolean step() {
        return groundPicker.getPickerHomeSensor();
    }

    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        groundPicker.resetFlipEncoder();
        groundPicker.setPickerAction(GroundPicker.PickerAction.IDLING);
        if (localDebug) {
            logger.info("Exiting Picker Home");
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.retractMotorPower = config.retractMotorPower;
        localDebug = config.debug;
    }

    public static class Config {
        public double retractMotorPower;
        public boolean debug;
    }
}
