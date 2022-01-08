package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class HoodHome extends Command implements Configurable<HoodHome.Config> {

    private Turret turret;
    private Config config;
    private boolean localDebug = false;

    public HoodHome(TatorRobot robot) {
        super("HoodHome");
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
        if (localDebug) {
            logger.info("Beginning Turret Hood Home");
        }
        if (turret.getCurrentTurretState() == Turret.TurretState.IDLING) {
            turret.hoodMotor.set(config.homePower);
            turret.homing = true;
            turret.setHoodTargetExtension(0);
        }
    }

    @Override
    public boolean step() {
        if (turret.getHoodSensor()) {
            turret.hoodMotor.stopMotor();
            return true;
        } else {
            turret.hoodMotor.set(config.homePower);
        }

        return turret.getCurrentTurretState() == Turret.TurretState.SHOOTING;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);

        if (interrupted || turret.getCurrentTurretState() != Turret.TurretState.IDLING) {
            turret.hoodEncoder.reset();
            if (localDebug) {
                logger.warn("Unable To Complete Turret Hood Home");
            }
            return;
        }

        if (localDebug) {
            logger.info("Turret Hood Home Complete");
        }
        turret.hoodEncoder.reset();
        turret.homing = false;
    }

    public static class Config {
        public double homePower;
        public boolean debug;
    }

}
