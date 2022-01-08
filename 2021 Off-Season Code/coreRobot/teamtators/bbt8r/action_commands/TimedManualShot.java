package org.teamtators.bbt8r.action_commands;

import com.ctre.phoenix.motorcontrol.ControlMode;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.BallChannel;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

public class TimedManualShot extends Command implements Configurable<TimedManualShot.Config> { // TOGGLE WHEN PRESSED

    private Config config;
    private SuperStructure superStructure;
    private Turret turret;
    private BallChannel ballChannel;
    private GroundPicker picker;
    private boolean localDebug = false;
    public double startTime;

    public TimedManualShot(TatorRobot robot) {
        super("ManualShot");
        turret = robot.getSubsystems().getTurret();
        superStructure = robot.getSubsystems().getSuperStructure();
        ballChannel = robot.getSubsystems().getBallChannel();
        picker = robot.getSubsystems().getGroundPicker();
    }

    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    @Override
    public void initialize()
    {
        super.initialize( true );

        turret.setTargetFlywheelSpeed(config.speed);
        superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.SHOOTING);
        picker.setPickerAction(GroundPicker.PickerAction.IDLING);

        ballChannel.resetBallChannelStateMachine();

        startTime = Timer.getTimestamp();

        if (config.clearPicker) {
            picker.setPickerAction(GroundPicker.PickerAction.CLEARING);
        }
    }

    @Override
    public boolean step() { // This command is meant to be backwards, to go forwards we must multiply by -1
        if (localDebug) {
            logger.info("Current Flywheel Speed: " + turret.getFlywheelSpeed());
            if (turret.isFlywheelOnTarget()) {
                logger.info("Flywheel On Target!");
            }
        }

        return superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.SHOOTING
                || Timer.getTimestamp() - startTime >= config.duration;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, true );

        turret.setTargetFlywheelSpeed(0);
        turret.flywheelMotor.set(ControlMode.Disabled, turret.flywheelMotor.get());
        turret.flywheelMotor.set(ControlMode.PercentOutput, 0);
        superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.SHOOTING);

        if (picker.getPickerAction() == GroundPicker.PickerAction.SHOOTING) 
        {
            // only change state to idling if we are in the shooting mode
            picker.setPickerAction(GroundPicker.PickerAction.IDLING);
        }

        // Not used
        // ballChannel.firing = false;

    }

    public static class Config {
        public double speed;
        public double duration;
        public boolean debug;
        public boolean clearPicker = false;
    }

}
