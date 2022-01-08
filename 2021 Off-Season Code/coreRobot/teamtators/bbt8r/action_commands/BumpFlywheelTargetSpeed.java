package org.teamtators.bbt8r.action_commands;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.config.Configurable;

public class BumpFlywheelTargetSpeed extends Command implements Configurable<BumpFlywheelTargetSpeed.Config> {
    private Turret turret;
    private double bumpFlywheelSpeed;
    private Config config;
    private boolean localDebug = false;

    public BumpFlywheelTargetSpeed(Turret turret) {
        super("BumpFlywheelTargetSpeed");
        this.turret = turret;
    }

    @Override
    public boolean step() {
        turret.setTargetFlywheelSpeed(turret.getFlywheelSpeed() + bumpFlywheelSpeed);
        return true;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        this.bumpFlywheelSpeed = config.bumpFlywheelSpeed;
        localDebug = config.debug;
    }

    public static class Config {
        public double bumpFlywheelSpeed;
        public boolean debug;
    }
}