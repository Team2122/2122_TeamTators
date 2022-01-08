package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.GroundPicker;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class RunFluffer extends Command implements Configurable<RunFluffer.Config> {
    private GroundPicker groundPicker;
    private SuperStructure superStructure;
    private boolean localDebug = false;
    private Config config;


    public RunFluffer(TatorRobot robot) {
        super("RunFluffer");
        groundPicker = robot.getSubsystems().getGroundPicker();
    }

    @Override
    protected void initialize() {
        super.initialize();
        logger.info("entered fluffing");
        groundPicker.setPickerAction(GroundPicker.PickerAction.FLUFFING);
    }

    @Override
    public boolean step() {
        return groundPicker.getPickerAction() != GroundPicker.PickerAction.FLUFFING;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        logger.info("Fluffing Finishing");
        superStructure.enterSuperStructureState(SuperStructure.SuperStructureState.IDLING);

    }

    public void configure(Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
