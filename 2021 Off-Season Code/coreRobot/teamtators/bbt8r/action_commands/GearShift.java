package org.teamtators.bbt8r.action_commands;

import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Drive;
import org.teamtators.common.commands.ConditionalRunCommand;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;

/**
 * Shift to a specific gear Starts when pressed
 */
public class GearShift extends Command implements Configurable<GearShift.Config> {
    private final Timer timer = new Timer();
    private Drive drive;

    private double timeout;
    private boolean localDebug = false;

    public GearShift(TatorRobot robot) {
        super("ShiftHigh");
        drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    public void initialize() {
        super.initialize(localDebug);

        timer.start();
        drive.setShiftButtonState(false);
    }

    @Override
    public boolean step() {
        drive.shiftGear(Drive.Gear.HIGH);
        if (localDebug) {
            logger.info("Shift State:" + drive.getShiftButtonState());
        }
        return timer.hasPeriodElapsed(timeout);
    }

    @Override
    public void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        if (localDebug) {
            logger.info("Shift State:" + drive.getShiftButtonState());
        }
    }

    @Override
    public void configure(Config config) {
        timeout = config.pressTimeSeconds;
        localDebug = config.debug;
    }

    public static class Config {
        public double pressTimeSeconds = 0.2;
        public boolean debug;
    }
}
