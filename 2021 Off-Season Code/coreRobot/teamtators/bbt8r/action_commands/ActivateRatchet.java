package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class ActivateRatchet extends Command implements Configurable<ActivateRatchet.Config> {
    private GroundPicker groundPicker;
    private Climber climber;
    private SuperStructure superStructure;
    private Config config;
    private boolean localDebug = false;


    public ActivateRatchet(TatorRobot robot) {
        super("ToggleRatchet");
        climber = robot.getSubsystems().getClimber();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        climber.activateRatchet();
    }

    @Override
    public boolean step() {
        climber.stopLiftMotor();
        return false;
    }

    public void configure(Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
