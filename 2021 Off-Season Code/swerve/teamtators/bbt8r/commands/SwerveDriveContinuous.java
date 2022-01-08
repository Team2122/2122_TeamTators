package org.teamtators.bbt8r.commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.SwerveInputProxy;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;

// There should be minimal code here. Pretty much all logic should be moved to the subsystem :)
public class SwerveDriveContinuous extends Command {

    private SwerveInputProxy inputProxy;

    public SwerveDriveContinuous(TatorRobot robot) {
        super("SwerveDriveContinuous");
        inputProxy = robot.getSubsystems().getInputProxy();
        validIn(RobotState.AUTONOMOUS, RobotState.TELEOP);
    }

    @Override
    public boolean step() {
        inputProxy.update();
        return false;
    }

}
