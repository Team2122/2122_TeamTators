package org.teamtators.bbt8r.action_commands;

import com.revrobotics.ControlType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class HookToBar extends Command implements Configurable<HookToBar.Config> {
    private static final Logger logger = LoggerFactory.getLogger(HookToBar.class);
    private final Climber climber;
    private double power;
    private double climbDesiredHeight;
    private boolean checkCurrent = false;
    private boolean localDebug = false;

    public HookToBar(TatorRobot robot) {
        super("HookToBar");
        climber = robot.getSubsystems().getClimber();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        if (localDebug) {
            logger.info("hook to bar started");
        }
        climber.deactivateRatchet();
        // climber.setLiftMotorPower(-power);
        climber.liftController.setReference(-4000, ControlType.kVelocity);
        checkCurrent = false;
    }

    @Override
    public boolean step() {

        if (localDebug) {
            logger.info("HB encoder ticks " + climber.liftEncoder.getRotations());
            logger.info("Lift Motor Power: " + climber.lift.getAppliedOutput());
            logger.info("Lift Current: " + climber.lift.getOutputCurrent());
        }

        if (climber.lift.getOutputCurrent() >= 60 || checkCurrent) {
            if (localDebug) {
                logger.info("WE ARE IN CHECK CURRENT");
            }
            climber.setLiftMotorPower(-1);
            checkCurrent = true;
        }
        if (climber.getLiftEncoderRotations() <= climbDesiredHeight || !climber.getLiftSensor()) {
            if (localDebug) {
                logger.info("if statement hit");
            }
            // climber.liftController.setReference(0, ControlType.kVelocity);
            climber.lift.stopMotor();
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        climber.activateRatchet();
        if (localDebug) {
            logger.info("HB encoder rotation finish : " + climber.getLiftEncoderRotations());
            logger.info("finished moving lift!");
        }
    }

    @Override
    public void configure(Config config) {
        this.power = config.power;
        this.climbDesiredHeight = config.climbDesiredHeight;
        localDebug = config.debug;
    }

    public static class Config {
        public double power;
        public double climbDesiredHeight;
        public boolean debug;
    }
}
