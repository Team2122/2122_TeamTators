package org.teamtators.bbt8r.action_commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class LowerArm extends Command implements Configurable<LowerArm.Config> {
    private static final Logger logger = LoggerFactory.getLogger(RaiseArm.class);
    private final Climber climber;
    private final Turret turret;

    // if this is true, then wait for it to be safe before raising arm
    private boolean waitForSafety;
    private boolean localDebug = false;

    public LowerArm(TatorRobot robot) {
        super("LowerArm");
        climber = robot.getSubsystems().getClimber();
        turret = robot.getSubsystems().getTurret();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        climber.lowerArm();
        if (localDebug) {
            logger.info("lowering the arm");
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
            logger.info("finished lowering arm, maybe");
        }
    }

    @Override
    public void configure(Config config) {
        this.waitForSafety = config.waitForSafety;
        localDebug = config.debug;
    }

    public static class Config {
        boolean waitForSafety;
        public boolean debug;
    }
}
