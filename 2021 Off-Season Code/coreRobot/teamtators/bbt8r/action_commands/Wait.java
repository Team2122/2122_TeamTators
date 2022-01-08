package org.teamtators.bbt8r.action_commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

public class Wait extends Command implements Configurable<Wait.Config> {

    private Config config;
    private double startTime;
    private boolean localDebug = false;

    public Wait() {
        super("Wait");
    }

    @Override
    public void initialize() {
        super.initialize(localDebug);
        startTime = Timer.getTimestamp();
    }

    @Override
    public boolean step() {
        return Timer.getTimestamp() - startTime > config.duration;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        localDebug = config.debug;
    }

    public static class Config {
        public double duration;
        public boolean debug;
    }
}
