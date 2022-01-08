package org.teamtators.bbt8r.action_commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.common.config.Configurable;
import org.teamtators.bbt8r.TatorRobot;

public class ClimbBump extends Command implements Configurable<ClimbBump.Config> {

    private Climber climber;
    private double initialRotations;
    private Config config;

    public ClimbBump(TatorRobot robot) {
        super("ClimbBump");
        climber = robot.getSubsystems().getClimber();
    }

    @Override
    public void initialize() {
        super.initialize();
        initialRotations = climber.getLiftEncoderRotations();
        climber.deactivateRatchet();
        climber.lift.set(config.power);
    }

    @Override
    public boolean step() {
        return climber.getLiftEncoderRotations() > initialRotations + config.change;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted);
        climber.stopLiftMotor();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double change;
        public double power;
    }

}
