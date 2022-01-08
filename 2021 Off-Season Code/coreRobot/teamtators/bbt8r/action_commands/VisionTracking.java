package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.bbt8r.subsystems.Vision;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class VisionTracking extends Command implements Configurable<VisionTracking.Config> { // TOGGLE WHEN PRESSED

    private SuperStructure superStructure;
    private Turret turret;
    private Vision vision;
    private boolean localDebug = false;
    private Config config;

    private Vision.VisionData visionData;

    public VisionTracking(TatorRobot robot) {
        super("VisionTracking");
        superStructure = robot.getSubsystems().getSuperStructure();
        turret = robot.getSubsystems().getTurret();
        vision = robot.getSubsystems().getVision();
    }

    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    @Override
    public void initialize() {
        super.initialize(true);
        superStructure.setTurretRotating(true, true);
        if (localDebug)
            logger.info("initializing");
        vision.setVisionStatus(true);
    }

    @Override
    public boolean step() {
        // Retrieve the data from the vision system
        visionData = vision.getData();

        // Check to make sure target is OK
        if (visionData.checkValidity() && visionData.valid) {
            // Set the target angle
            if (localDebug) {
                logger.info("Setting new Target Angle");
            }
            turret.setTargetTurretAngle(visionData.deltaAngle + visionData.initialAngle);
        } else {
            // Don't move the turret angle
            turret.setTargetTurretAngle(turret.getTurretAngle());
        }

        // IS THIS A PROBLEM ????
        // turret.getCurrentState() != Turret.TurretState.ROTATING;

        return false;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        vision.setVisionStatus(false);
        superStructure.setTurretRotating(false, false);
    }

    public static class Config {
        public boolean debug;
    }

}
