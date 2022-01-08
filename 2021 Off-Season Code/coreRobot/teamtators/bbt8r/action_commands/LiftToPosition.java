package org.teamtators.bbt8r.action_commands;

import com.revrobotics.ControlType;
import com.sun.source.tree.PackageTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.bbt8r.TatorRobot;
import org.teamtators.bbt8r.subsystems.Climber;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;

public class LiftToPosition extends Command implements Configurable<LiftToPosition.Config> {
    private static final Logger logger = LoggerFactory.getLogger(LiftToPosition.class);
    private final Climber climber;
    private double power;
    private double liftDesiredHeight;
    private double liftUpperBound;
    private boolean localDebug = false;

    public LiftToPosition(TatorRobot robot) {
        super("LiftToPosition");
        if (localDebug)
            logger.info("Starting Lift to Position");
        climber = robot.getSubsystems().getClimber();
    }

    @Override
    protected void initialize() {
        super.initialize(localDebug);
        climber.deactivateRatchet();

        // climber.liftController.setReference(100, ControlType.kPosition);

    }

    @Override
    public boolean step() {
        // Checking to see if the bar is over extended
        if (climber.getLiftEncoderRotations() >= liftDesiredHeight) {
            if (localDebug) {
                logger.info("LTP Encoder rotations : " + climber.getLiftEncoderRotations());
            }
            climber.stopLiftMotor();
            return true;
        } else if (climber.getLiftEncoderRotations() >= liftUpperBound) {
            if (localDebug) {
                logger.info("LTP Encoder rotations : " + climber.getLiftEncoderRotations());
            }
            climber.stopLiftMotor();
            return true;
        } else {
            // Start driving the lift upwards
            climber.lift.set(power);
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted, localDebug);
        // climber.activateRatchet();
        for (int i = 0; i < 10; i++) {
            logger.info("Encoder Rotations when Finishing: " + climber.getLiftEncoderRotations());
        }
        if (localDebug) {
            logger.info("finished moving lift!");
        }
    }

    @Override
    public void configure(Config config) {
        this.liftDesiredHeight = config.liftDesiredHeight;
        this.liftUpperBound = config.liftUpperBound;
        this.power = config.power;
        localDebug = config.debug;

    }

    public static class Config {
        public double liftDesiredHeight;
        public double liftUpperBound;
        public double power;
        public boolean debug;

    }
}
