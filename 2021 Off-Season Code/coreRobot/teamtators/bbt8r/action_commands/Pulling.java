package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.BallChannel;
import org.teamtators.bbt8r.subsystems.SuperStructure;
import org.teamtators.common.scheduler.Command;

public class Pulling extends Command {

    private BallChannel ballChannel;
    private SuperStructure superStructure;

    public Pulling(TatorRobot robot) {
        super("Pulling");
        ballChannel = robot.getSubsystems().getBallChannel();
        superStructure = robot.getSubsystems().getSuperStructure();
    }

    @Override
    public boolean step() {
        // What is this code doing !!!!!!!!!!!!!!!!!
        // ballChannel.setHorizontalRollersPower(ballChannel.config.horizontalRollerPower);
        return superStructure.getCurrentSuperStructureState() != SuperStructure.SuperStructureState.IDLING;
    }
}
