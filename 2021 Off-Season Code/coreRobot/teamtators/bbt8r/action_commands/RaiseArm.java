package org.teamtators.bbt8r.action_commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class RaiseArm extends Command implements Configurable<RaiseArm.Config> {
    private static final Logger logger = LoggerFactory.getLogger(RaiseArm.class);
    private final Climber climber;
    private final Turret turret;
    private final GroundPicker picker;
    public Config config;
    private boolean localDebug = false;

    // if this is true, then wait for it to be safe before raising arm
    private boolean waitForSafety;

    public RaiseArm(TatorRobot robot) {
        super("RaiseArm");
        climber = robot.getSubsystems().getClimber();
        turret = robot.getSubsystems().getTurret();
        picker = robot.getSubsystems().getGroundPicker();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        picker.stopFlipMotor();
        climber.raiseArm();
        if (localDebug) {
            logger.info("raising the arm");
        }
    }

    @Override
    public boolean step() {
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        if (localDebug) {
            logger.info("finished raising arm, maybe");
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.waitForSafety = config.waitForSafety;
        localDebug = config.debug;
    }

    public static class Config {
        public boolean waitForSafety;
        public boolean debug;
    }
}
