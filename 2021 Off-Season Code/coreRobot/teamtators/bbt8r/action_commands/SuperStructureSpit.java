package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class SuperStructureSpit extends Command implements Configurable<SuperStructureSpit.Config> {
    private SuperStructure superStructure;
    private Turret turret;
    private boolean localDebug = false;
    private Config config;

    public SuperStructureSpit(TatorRobot robot) {
        super("SuperStructureSpit");
        superStructure = robot.getSubsystems().getSuperStructure();
        turret = robot.getSubsystems().getTurret();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.SPITTING);
        turret.setTargetFlywheelSpeed(turret.config.spittingSpeed);
    }

    @Override
    public boolean step() {
        return (superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.SPITTING);
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.SPITTING);
    }

    public void configure (Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
