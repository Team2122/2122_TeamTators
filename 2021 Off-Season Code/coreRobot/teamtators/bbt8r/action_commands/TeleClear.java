package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.common.scheduler.Command;

public class TeleClear extends Command {
    private GroundPicker picker;
    private SuperStructure superStructure;

    public TeleClear(TatorRobot robot) {
        super("SuperStructureClear");
        picker = robot.getSubsystems().getGroundPicker();
        superStructure = robot.getSubsystems().getSuperStructure();
    }

    @Override
    public boolean step() {

        if(superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.CLEARING){
            superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.CLEARING);
        } else {
            superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.IDLING);

            //picker.setPickerAction(GroundPicker.PickerAction.TELE_CLEARING);
        }
        return true;
    }

}
