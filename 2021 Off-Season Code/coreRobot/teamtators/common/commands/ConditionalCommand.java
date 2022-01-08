package org.teamtators.common.commands;

import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

import java.util.ArrayList;
import java.util.List;

public class ConditionalCommand extends Command implements Configurable<ConditionalCommand.Config> {
    private Command operand;
    private Config config;
    private Command yes;
    private Command no;
    private Command selected;
    private boolean hasStarted;
    private ConfigCommandStore commandStore;
    private boolean initialized;
    private boolean cancel;

    public ConditionalCommand(TatorRobotBase robot) {
        super("Conditional");
        commandStore = robot.getCommandStore();
    }

    public void initialize() {
        super.initialize();
        initialized = false;
        cancel = false;
        hasStarted = false;
        operand = commandStore.getCommand(config.command);
        yes = commandStore.getCommand(config.yes);
        no = commandStore.getCommand(config.no);
        startCommand(operand);
    }

    public boolean step() {
        if (selected == null) {
            boolean yn = operand.step();
            if(yn) {
                selected = yes;
            } else {
                operand.cancel();
                selected = no;
            }
            logger.info("Selected command {}", selected.getName());
        }
        if (cancel) {
            selected.finishRun(true);
            this.cancel();
            return true;
        }
        if (!initialized) {
            releaseRequirements(selected.getRequirements());
            if (selected.isRunning()) {
                if (selected.getContext() == this && selected.checkRequirements()) {
//                    logger.trace("Command was already initialized");
                    initialized = true;
                } else {
//                    logger.trace("Command was already running, canceling");
                    selected.cancel();
                    return false;
                }
            } else if (selected.startRun(this)) {
//                logger.trace("Command initialized");
                initialized = true;
            }
        }
        if (initialized) {
            boolean finished = selected.step();
            if (finished) {
                selected.finishRun(false);
                return true;
            }
        } else {
//            logger.trace("Command was not initialized");
        }
        if (cancel) {
            selected.finishRun(true);
            this.cancel();
            return true;
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        if (interrupted && selected != null && selected.isRunning()) {
            selected.finishRun(true);
        }
    }

    @Override
    public void cancelCommand(Command command) {
        if (command == selected) {
            cancel = true;
        } else {
            super.cancelCommand(command);
        }
    }

    @Override
    public void updateRequirements() {
        if (config == null) {
            return;
        }
        List<String> commandNames = new ArrayList<>();
        commandNames.add(config.command);
        commandNames.add(config.yes);
        commandNames.add(config.no);
        for (String commandName : commandNames) {
            Command command;
            try {
                command = commandStore.getCommand(commandName);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if(command.getRequirements() == null)
                continue;
            requiresAll(command.getRequirements());
        }
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        updateRequirements();
    }

    public static class Config {
        public String command;
        public String yes;
        public String no;
    }
}
