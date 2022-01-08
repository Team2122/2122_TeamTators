package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class SuperStructureClear extends Command implements Configurable<SuperStructureClear.Config> {

    private SuperStructure superStructure;
    private GroundPicker picker;
    private boolean localDebug = false;
    private Config config;

    public SuperStructureClear(TatorRobot robot) {
        super("SuperStructureClear");
        superStructure = robot.getSubsystems().getSuperStructure();
        picker = robot.getSubsystems().getGroundPicker();
    }

    @Override
    protected void initialize() {
        super.initialize(true);
        // Should we be using this?
//       superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.CLEARING);
    }

    @Override
    public boolean step() {
        // This function is only ever run once, line 39 returns true
//        return (superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.CLEARING);
        if (picker.getPickerAction() == GroundPicker.PickerAction.CLEARING) {
            picker.setPickerAction(GroundPicker.PickerAction.IDLING);
        } else {
            picker.setPickerAction(GroundPicker.PickerAction.CLEARING);
        }

        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, true);
//        superStructure.exitSuperStructureState(SuperStructure.SuperStructureState.CLEARING);
    }

    public void configure (Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
