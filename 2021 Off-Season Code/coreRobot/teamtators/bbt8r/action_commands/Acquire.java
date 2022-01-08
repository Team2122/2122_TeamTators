package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.bbt8r.subsystems.Vision;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

public class Acquire extends Command implements Configurable<Acquire.Config> {

    private SuperStructure superStructure;
    private Turret turret;
    private Vision vision;
    private Config config;

    private Timer timer;
    private Vision.VisionData visionData;

    private boolean localDebug = false;

    public Acquire(TatorRobot robot) {
        super("Acquire");
        superStructure = robot.getSubsystems().getSuperStructure();
        turret = robot.getSubsystems().getTurret();
        vision = robot.getSubsystems().getVision();
        timer = new Timer();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    @Override
    public void initialize() {
        super.initialize(true);
        superStructure.setTurretRotating(true, true);
        vision.setVisionStatus(true);
        timer.restart();
    }

    @Override
    public boolean step() {
        visionData = vision.getData();
        
        if (visionData.checkValidity() && visionData.valid) {
            turret.setTargetTurretAngle(visionData.deltaAngle + visionData.initialAngle);
        } else {
            turret.setTargetTurretAngle(turret.getTurretAngle());
        }

        if (turret.isTurretAtAngle()) {
            if (timer.isRunning()) {
                if (timer.hasPeriodElapsed(config.insuranceTime)) {
                    return true;
                }
            } else {
                timer.start();
            }
        } else {
            if (timer.isRunning()) {
                timer.stop();
            }
        }

//        return turret.getCurrentTurretState() != Turret.TurretState.ROTATING;
        return false;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        vision.setVisionStatus(false);
        superStructure.setTurretRotating(false, false);
    }

    public static class Config {
        public double insuranceTime; // Seconds
        public boolean debug;
    }

}
