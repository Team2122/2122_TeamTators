package org.teamtators.bbt8r.action_commands;

import com.revrobotics.ControlType;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class TurretToAngle extends Command implements Configurable<TurretToAngle.Config> {

    private Turret turret;
    private SuperStructure superStructure;
    private boolean localDebug = false;

    public Config config;

    public TurretToAngle(TatorRobot robot) {
        super("TurretToAngle");
        turret = robot.getSubsystems().getTurret();
        superStructure = robot.getSubsystems().getSuperStructure();
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    @Override
    public void initialize() {

        if (localDebug)
            logger.info("TurretToAngle Initialize: " + config.angle);

        turret.setTargetTurretAngle(config.angle);
        superStructure.setTurretRotating(true, true);

        turret.printTurretTargetRotations();
        turret.printTurretRotations();
    }

    @Override
    public boolean step() {

//        double currentRotations = turret.getRotationEncoder() / turret.hoodMotor.getEncoder().getCountsPerRevolution();
//        double addedRotations = (targetTicks - turret.getRotationEncoder()) / turret.hoodMotor.getEncoder().getCountsPerRevolution();
//        turret.rotationController.setReference(currentRotations + addedRotations, ControlType.kSmartMotion);

        // if (Math.random() > .97) {
        // System.out.println("TurretToAngle Step");
        // turret.printTurretTargetRotations();
        // turret.printTurretRotations();
        // }

        return turret.isTurretAtAngle() || turret.getCurrentTurretState() != Turret.TurretState.ROTATING;
    }

    @Override
    public void finish(boolean interrupted) {
        if (localDebug)
            logger.info("TurretToAngle Finish");

        super.finish(interrupted, localDebug);
        superStructure.setTurretRotating(false, false);
        turret.printTurretRotations();
    }

    public static class Config {
        public double angle;
        public boolean debug;
    }
}
