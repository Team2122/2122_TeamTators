package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class WallExtend extends Command implements Configurable<WallExtend.Config> {
    private GroundPicker groundPicker;
    private SuperStructure superStructure;
    private boolean localDebug = false;
    private Config config;

    public WallExtend(TatorRobot robot) {
        super("PickerPick");
        superStructure = robot.getSubsystems().getSuperStructure();
        groundPicker = robot.getSubsystems().getGroundPicker();

    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        groundPicker.setPickerAction(GroundPicker.PickerAction.WALL_EXTEND);
    }

    @Override
    public boolean step() {
        return  groundPicker.getPickerAction() != GroundPicker.PickerAction.WALL_EXTEND;
        //superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.PICKING
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        //groundPicker.setPickerAction(GroundPicker.PickerAction.PICKING);
    }

    public void configure (Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
