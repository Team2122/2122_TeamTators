package org.teamtators.sassitator.commands;

import frc.robot.RobotContainer;
import org.teamtators.sassitator.Command;

public class InstantCommand extends Command {

    private final Runnable runnable;

    public InstantCommand(RobotContainer robotContainer, Runnable runnable) {
        super(robotContainer);
        this.runnable = runnable;
    }

    @Override
    public void initialize() {
        super.initialize();
        runnable.run();
    }

    @Override
    public boolean isFinished() {
        return true;
    }

}
