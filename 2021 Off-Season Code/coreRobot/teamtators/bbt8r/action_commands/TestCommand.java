package org.teamtators.bbt8r.action_commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

public class TestCommand extends Command implements Configurable<TestCommand.Config> {

    private boolean localDebug = false;
    double lifetime = 5; // In Seconds
    double startTime;
    private Config config;

    public TestCommand() {
        super("TestCommand");
    }

    @Override
    public void initialize() {
        super.initialize(localDebug);
        if (localDebug) {
            logger.info("Test Command Initializing");
        }
        startTime = Timer.getTimestamp();
    }

    @Override
    public boolean step() {
        if (localDebug) {
            logger.info("Test Command Running");
        }

        if (Timer.getTimestamp() - startTime > lifetime) {
            return true;
        }

        return false;
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        if (localDebug) {
            logger.info("Test Command Finishing");
        }
    }

    public void configure(Config config) {
        this.config = config;
        this.localDebug = config.debug;
    }

    public static class Config {
        public boolean debug;
    }
}
