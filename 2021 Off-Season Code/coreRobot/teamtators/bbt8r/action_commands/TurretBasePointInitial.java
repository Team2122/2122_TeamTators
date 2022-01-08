package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;

public class TurretBasePointInitial extends Command implements Configurable<TurretBasePointInitial.Config> {

    Turret turret;
    double startTime;
    Config config;
    private boolean localDebug = false;

    public TurretBasePointInitial(TatorRobot robot) {
        super("TurretBasePointInitial");
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
        startTime = Timer.getTimestamp();
    }

    @Override
    public boolean step() {
        if (turret.getCurrentTurretState() != Turret.TurretState.ROTATING) {
            turret.updateBasePoint();
        }
        return Timer.getTimestamp() - startTime > config.duration;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        if (localDebug) {
            logger.info("Exiting TurretBasePointInitial");
        }
    }

    @Override
    public boolean isValidInState(RobotState robotState) {
        return true;
    }

    public static class Config {
        public double duration; // Seconds
        public boolean debug;
    }
}
