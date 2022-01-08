package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.bbt8r.subsystems.Vision;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class VisionShoot extends Command implements Configurable<VisionShoot.Config> { // TOGGLE WHEN PRESSED

    private SuperStructure superStructure;
    private Turret turret;
    private Vision vision;
    private Vision.VisionData visionData;
    private Config config;
    private boolean localDebug = false;

    public VisionShoot(TatorRobot robot) {
        super("VisionShoot");
        superStructure = robot.getSubsystems().getSuperStructure();
        turret = robot.getSubsystems().getTurret();
        vision = robot.getSubsystems().getVision();
    }

    public void configure(Config config) {
        this.config = config;
        localDebug =  config.debug;
    }

    @Override
    public void initialize() {
        super.initialize(localDebug);
        vision.setVisionStatus(true);
        superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.SHOOTING);
    }

    @Override
    public boolean step() {
        visionData = vision.getData();
        turret.setTargetFlywheelSpeed(vision.distanceToFlywheelSpeed(visionData.distance));
        return superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.SHOOTING;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.SHOOTING);
        vision.setVisionStatus(false);
    }
    public static class Config {
        public boolean debug;
    }

}
