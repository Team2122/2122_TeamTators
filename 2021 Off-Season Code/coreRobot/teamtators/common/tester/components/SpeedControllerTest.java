package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.helpers.SpeedControllerConfig;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class SpeedControllerTest extends ManualTest {

    protected SpeedController motor;
    private boolean half;
    private double axisValue;
    private PowerDistributionPanel pdp;
    private SpeedControllerConfig motorConfig;
    private double upperBound = 1;
    private double lowerBound = -1;

    public SpeedControllerTest(String name, SpeedController motor) {
        super(name);
        this.motor = motor;
    }

    public SpeedControllerTest(String name, SpeedController motor, double upperBound, double lowerBound) {
        this(name, motor);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public SpeedControllerTest(String name, SpeedController motor, PowerDistributionPanel pdp,
                               SpeedControllerConfig motorConfig) {
        this(name, motor);
        this.pdp = pdp;
        this.motorConfig = motorConfig;
    }

    @Override
    public void start() {
        printTestInstructions("Push joystick in direction to move (forward +, backward -), start to drive at 50% speed");
        if (this.pdp != null) {
            printTestInstructions("Press A to get current usage");
        }
        half = false;
        axisValue = 0;
    }

    private double getRawSpeed() {
        if (half) {
            return 0.5;
        } else {
            return axisValue;
        }
    }

    private double getSpeed() {
        double speed = getRawSpeed();
        speed = (speed > upperBound ? upperBound : speed);
        speed = (speed < lowerBound ? lowerBound : speed);
        return speed;
    }

    public double getCurrent() {
        return motorConfig.getTotalCurrent(pdp);
    }

    @Override
    public void update(double delta) {
        motor.set(getSpeed());
    }

    @Override
    public void stop() {
        motor.set(0);
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if(button == LogitechF310.Button.START) half = true;
        else if (button == LogitechF310.Button.A && pdp != null) {
            logger.info("Total current usage: " + this.getCurrent());
        }
    }

    @Override
    public void onButtonUp(LogitechF310.Button button) {
        if (button == LogitechF310.Button.START) half = false;
    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }
}
