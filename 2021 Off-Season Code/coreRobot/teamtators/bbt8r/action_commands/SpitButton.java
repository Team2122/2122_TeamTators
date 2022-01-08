package org.teamtators.bbt8r.action_commands;

import com.ctre.phoenix.motorcontrol.ControlMode;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class SpitButton extends Command implements Configurable<SpitButton.Config> {
    private GroundPicker groundPicker;
    private SuperStructure superStructure;
    private Turret turret;
    private Config config;

    private boolean actuallyPicking;
    private boolean localDebug = false;

    public SpitButton(TatorRobot robot) {
        super("PickerPick");
        superStructure = robot.getSubsystems().getSuperStructure();
        groundPicker = robot.getSubsystems().getGroundPicker();
        turret = robot.getSubsystems().getTurret();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        if (localDebug) {
            logger.info("entered wall pick command");
        }

        actuallyPicking = SuperStructure.SuperStructureState.PICKING == superStructure.getCurrentSuperStructureState();

        if (!actuallyPicking) {
            if (localDebug) {
                logger.info("Current State : SPITTING");
            }
            superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.SPITTING);
        } else {
            if (localDebug) {
                logger.info("Current State : WALL PICKING");
            }
            // groundPicker.setPickerAction(GroundPicker.PickerAction.WALL_PICKING);
        }
    }

    @Override
    public boolean step() {
        return superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.SPITTING
                || actuallyPicking;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);

        if (!actuallyPicking) {
            if (localDebug) {
                logger.info("Spitting finishing");
            }
            superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.SPITTING);
            turret.setTurretState(Turret.TurretState.IDLING);
            turret.setTargetFlywheelSpeed(0);
            turret.flywheelMotor.set(ControlMode.Disabled, turret.flywheelMotor.get());
            turret.flywheelMotor.set(ControlMode.PercentOutput, 0);
        }
    }

    public void configure (Config config){
        this.config = config;
        localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
