package org.teamtators.common.tester.components;

import org.teamtators.common.control.SparkMaxPIDController;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.datalogging.DataCollector;
import org.teamtators.common.datalogging.LogDataProvider;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.hw.TatorSparkMax;
import org.teamtators.common.tester.ManualTest;

import java.util.Arrays;
import java.util.List;

public class SparkMaxMotionTest extends ManualTest {
    private final SparkMaxPIDController follower;
    private final NEOEncoder encoder;
    private final TatorSparkMax motor;
    private Runnable onStop;
    private Runnable onStart;

    private double stickInput;
    private boolean applyStick;
    private boolean run;
    private double targetPosition;
    private double power;
    private double position;
    private double velocity;
    private double lastVelocity;
    private double acceleration;
    private double firstPos;
    private double secondPos;
    private boolean moving;
    private boolean velocityPID;

    private final DataCollector dataCollector;
    private LogDataProvider logDataProvider = new LogDataProvider() {
        @Override
        public String getName() {
            return SparkMaxMotionTest.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("tpos", "appliedOutput", "position", "velocity");
        }

        @Override
        public List<Object> getValues() {
            return Arrays.asList(targetPosition, power, position, velocity);
        }
    };

    public SparkMaxMotionTest(String name, SparkMaxPIDController controller, double firstPos, double secondPos, boolean velocityPID) {
        super(name);
        this.follower = controller;
        this.motor = controller.getSparkMax();
        this.encoder = motor.getNeoEncoder();
        dataCollector = DataCollector.getDataCollector();
        this.firstPos = firstPos;
        this.secondPos = secondPos;
        this.velocityPID = velocityPID;
    }

    public SparkMaxMotionTest(SparkMaxPIDController follower, Runnable onStart, Runnable onStop, double firstPos, double secondPos) {
        this(follower.getName() + "Calibration", follower, firstPos, secondPos, false);
        this.onStart = onStart;
        this.onStop = onStop;
    }

    @Override
    public void start() {
        if (onStart != null) onStart.run();
        run = false;
        applyStick = false;
        logger.info("B to set target from stick, hold X to run at set target, hold Y to run at joystick target.");
//        logger.info("Hold START to move to {}, hold BACK to move to {}", firstPos, secondPos);
    }

    @Override
    public void update(double delta) {
        position = encoder.getDistance();
        velocity = encoder.getRate();
        acceleration = (velocity - lastVelocity) / delta;
        lastVelocity = velocity;
        if (applyStick) {
            targetPosition = stickInput;
        }
        if (!moving) {
            if (run) {
                if(velocityPID) {
                    follower.setVelocitySetpoint(targetPosition);
                } else {
                    follower.moveToPosition(targetPosition);
                }
            } else {
                follower.stop();
            }
        } else {
            power = motor.getAppliedOutput();
        }
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case B:
                targetPosition = stickInput;
                logger.info(String.format("Set target to %.3f", targetPosition));
                break;
            case X:
                logger.info(String.format("Running at power %.3f", targetPosition));
                run = true;
                dataCollector.startProvider(logDataProvider);
                break;
            case Y:
                logger.info("Running at stick target");
                run = applyStick = true;
                dataCollector.startProvider(logDataProvider);
                break;
        }
    }

    @Override
    public void onButtonUp(LogitechF310.Button button) {
        switch (button) {
            case START:
            case BACK:
            case X:
            case Y:
                logger.info("Stopped running");
                run = applyStick = moving = false;
                dataCollector.stopProvider(logDataProvider);
                break;
        }
    }

    @Override
    public void updateAxis(double value) {
        this.stickInput = ((value + 1) / 2) * secondPos;
    }

    @Override
    public void stop() {
        if (onStop != null) onStop.run();
        dataCollector.stopProvider(logDataProvider);
        follower.stop();
    }
}
