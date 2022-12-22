package org.teamtators.sassitator;

import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.Robot.EnumRobotState;
import frc.robot.RobotContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;

public abstract class Command extends CommandBase implements Debuggable {

    protected Logger logger;
    private boolean debug;
    private final EnumSet<EnumRobotState> validStates;

    private Command exitBehavior;

    public Command(RobotContainer robotContainer) {
        setName(getClass().getSimpleName());
        logger = LoggerFactory.getLogger(getName());
        robotContainer.getCommands().put(getName(), this);
        robotContainer.getDebuggableRegistry().registerDebuggable(this);
        validStates = EnumSet.of(EnumRobotState.Teleop, EnumRobotState.Autonomous);
//        logger.info("Created");
    }

    @Override
    public void initialize() {
        logger.info("Initializing");
    }

    @Override
    public void end(boolean interrupted) {
        if (interrupted) {
            logger.info("Interrupted");
        } else {
            logger.info("Ending");
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        logger = LoggerFactory.getLogger(name);
    }

    public void setExitBehavior(Command command) {
        this.exitBehavior = command;
    }

    public Command getExitBehavior() {
        return exitBehavior;
    }

    public boolean shouldStopCommandGroup() {
        return false;
    }

    public final void setValidity(EnumRobotState... validStates) {
        this.validStates.clear();
        this.validStates.addAll(Arrays.asList(validStates));
    }

    public final boolean validIn(EnumRobotState robotState) {
        return validStates.contains(robotState);
    }

    @Override
    public final boolean runsWhenDisabled() {
        return validIn(EnumRobotState.Disabled);
    }

    @Override
    public final void debugOn() {
        debug = true;
    }

    @Override
    public final void debugOff() {
        debug = false;
    }

    @Override
    public final void debugToggle() {
        debug = !debug;
    }

    public final boolean isDebugging() {
        return debug;
    }

}
