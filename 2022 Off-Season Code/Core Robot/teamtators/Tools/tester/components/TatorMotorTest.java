package org.teamtators.Tools.tester.components;

import org.teamtators.Tools.tester.ManualTest;
import org.teamtators.Util.TatorMotor;
import org.teamtators.Controllers.XBOXController;

import edu.wpi.first.wpilibj.PowerDistribution;

public class TatorMotorTest extends ManualTest{

    private TatorMotor motor;
   // private TatorMotor lMotor;
  //  private TatorMotor rMotor;
    private boolean half;
    private double axisValue;
    private PowerDistribution pdp;
    private int motorChannel = -1;
    private double upperBound = 1;
    private double lowerBound = -1;

    public TatorMotorTest(String name, TatorMotor motor) {
        super(name);
        this.motor = motor;
    }

    public TatorMotorTest(String name, TatorMotor motor, double maxPowerValue) {
        super(name);
        upperBound = maxPowerValue;
        lowerBound = -maxPowerValue;
        this.motor = motor;
    }

    public TatorMotorTest(String name, TatorMotor motor, double upperBound, double lowerBound) {
        this(name, motor);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.motor = motor;
    }

    public TatorMotorTest(String name, TatorMotor motor, PowerDistribution pdp, int motorChannel) {
        this(name, motor);
        this.pdp = pdp;
        this.motorChannel = motorChannel;
        this.motor = motor;
    }

    @Override
    public void start() {
        printTestInstructions("Push joystick in direction to move (forward +, backward -), start to drive at 50% speed");
        if (this.pdp != null) {
            printTestInstructions("Press A to get current usage");
        }
        printTestInstructions("Press 'X' to display the current encoder values, 'Y' to reset the encoder values");

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
        if (motorChannel == -1) {
            return 0;
        }
        else {
            return pdp.getCurrent(motorChannel); // check if this is right
        }
    }

    @Override
    public void update(double delta) {
        motor.setPercentOutput(getSpeed());
    }

    @Override
    public void stop() {
        motor.stop();
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if(button == XBOXController.Button.kSTART) half = true;
        else if (button == XBOXController.Button.kA && pdp != null) {
            System.out.println("Total current usage: " + this.getCurrent());
            //logger.info("Total current usage: " + this.getCurrent());
        }
        else if (button == XBOXController.Button.kY) {
            motor.setEncoderPosition(0);
            printTestInfo("Encoder reset");
        } else if (button == XBOXController.Button.kX) {
            printTestInfo(String.format("Rate: %.3f, Rotations: %.3f ",
            motor.getVelocity(), motor.getPosition()));
        }
    }

    @Override
    public void onButtonUp(XBOXController.Button button) {
        if (button == XBOXController.Button.kSTART) half = false;
    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }
}