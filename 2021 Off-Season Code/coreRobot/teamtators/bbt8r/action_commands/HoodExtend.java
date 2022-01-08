package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class HoodExtend extends Command implements Configurable<HoodExtend.Config> {

    private Turret turret;
    public Config config;
    private boolean localDebug = false;

    public HoodExtend(TatorRobot robot) {
        super("HoodExtend");
        turret = robot.getSubsystems().getTurret();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    @Override
    public void initialize() {
        super.initialize(localDebug);
        turret.setHoodTargetExtension(config.percentExtension);
        if (localDebug) {
            logger.info("Hood Extend Beginning");
        }
    }

    @Override
    public boolean step() {
        turret.setHoodMotorExtension();

        return turret.isHoodAtExtension();
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        if (localDebug) {
            logger.info("Hood Extended");
        }
        turret.hoodMotor.stopMotor();
    }

    public static class Config {
        public double percentExtension;
        public boolean debug;
    }

}
