package org.teamtators.common.scheduler;

public class CommandRun {
    Command command;
    boolean initialized = false;
    boolean cancel = false;
    CommandRunContext context = null;

    public CommandRun(Command command) {
        this.command = command;
    }
}
