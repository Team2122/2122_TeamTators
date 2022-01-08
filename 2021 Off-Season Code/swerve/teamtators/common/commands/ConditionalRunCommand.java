package org.teamtators.common.commands;

import org.teamtators.common.scheduler.Command;

import java.util.function.Supplier;

/**
 * A command that only runs with the given condition (extend this class to create a command)
 */
public abstract class ConditionalRunCommand extends Command {
    private Supplier<Boolean> condition;
    private boolean continuousMode;

    /**
     *
     * @param commandName name of this command
     * @param condition supplier returning whether or not the command should be run
     * @param continuousMode true = check during continuous step; false = check at beginning of step and abort if the condition is not fulfilled
     */
    public ConditionalRunCommand (String commandName, Supplier<Boolean> condition, boolean continuousMode) {
        super(commandName);
        this.condition = condition;
        this.continuousMode = continuousMode;
    }
    @Override
    public boolean step () {
        boolean doRun = condition.get();
        if (doRun) {
            return conditionalRun();
        } else if (!continuousMode) {
            return true; // finished
        }
        return false;
    }

    /**
     * the step function, but only run when the condition is true
     */
    public abstract boolean conditionalRun ();
}
