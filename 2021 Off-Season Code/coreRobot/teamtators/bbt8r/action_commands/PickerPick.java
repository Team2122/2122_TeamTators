package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.BallChannel;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.bbt8r.subsystems.GroundPicker.PickerAction;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.bbt8r.subsystems.GroundPicker;

public class PickerPick extends Command implements Configurable<PickerPick.Config> {

    private GroundPicker groundPicker;
    private SuperStructure superStructure;
    private BallChannel ballChannel;
    
    private boolean localDebug = false;
    private Config config;

    public PickerPick(TatorRobot robot) {
        super("PickerPick");
        superStructure = robot.getSubsystems().getSuperStructure();
        groundPicker = robot.getSubsystems().getGroundPicker();
        ballChannel = robot.getSubsystems().getBallChannel();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        ballChannel.resetBallChannelStateMachine();
        superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.PICKING);
    }

    @Override
    public boolean step() {
        // If the ground picker is not in picking and this command is run we put it in the PICKING state
        // Else when the toggle is ended we exit out of the picking state in the superstructure
        if (groundPicker.getPickerAction() != PickerAction.PICKING) {
            superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.PICKING);
        } else {
            superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.PICKING);
        }
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
    }

    public void configure (Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }

}