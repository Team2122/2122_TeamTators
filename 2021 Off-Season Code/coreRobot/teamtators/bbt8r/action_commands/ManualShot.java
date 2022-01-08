package org.teamtators.bbt8r.action_commands;

import com.ctre.phoenix.motorcontrol.ControlMode;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.BallChannel;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.Turret;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class ManualShot extends Command implements Configurable<ManualShot.Config> { // TOGGLE WHEN PRESSED

    public Config config;
    private SuperStructure superStructure;
    private Turret turret;
    private BallChannel ballChannel;
    private GroundPicker picker;
    private boolean localDebug = false;

    public ManualShot(TatorRobot robot) {
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
    public void initialize() {
        super.initialize();

        // Turn on the flywheel, ang get ready for shooting

        ballChannel.resetBallChannelStateMachine();

        turret.setTargetFlywheelSpeed(config.speed);
        superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.SHOOTING);
        picker.setPickerAction(GroundPicker.PickerAction.SHOOTING);

    }

    @Override
    public boolean step() {

        // Only exit out of step when we are NOT in picking
        boolean return_state = ( superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.SHOOTING ) ;
        return return_state ;

    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        
        // Turn off the motors, etc
        turret.setTargetFlywheelSpeed(0);
        turret.flywheelMotor.set(ControlMode.Disabled, turret.flywheelMotor.get());
        turret.flywheelMotor.set(ControlMode.PercentOutput, 0);
        
        superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.SHOOTING);

        if (picker.getPickerAction() == GroundPicker.PickerAction.SHOOTING) {
            // only change state to idling if we are in the shooting mode
            picker.setPickerAction(GroundPicker.PickerAction.IDLING);
        }

        // Commenting, this is not used
        // ballChannel.firing = false;
    }

    public static class Config {
        public double speed;
        public boolean debug;
    }

}
