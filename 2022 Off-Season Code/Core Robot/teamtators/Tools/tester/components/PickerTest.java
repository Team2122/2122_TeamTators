package org.teamtators.Tools.tester.components;

import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.motorcontrol.PWMVictorSPX;
import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;
import org.teamtators.Util.TatorMotor;

public class PickerTest extends ManualTest {
    private TatorMotor motor;
    // private TatorMotor lMotor;
    //  private TatorMotor rMotor;
    private boolean half;
    private double axisValue;
    private PowerDistribution pdp;
    private int motorChannel = -1;
    private double upperBound = 1;
    private double lowerBound = -1;
    private DutyCycleEncoder encoder;

    public PickerTest(String name, TatorMotor motor, double maxPowerValue, DutyCycleEncoder dutyCycleEncoder) {
        super(name);
        upperBound = maxPowerValue;
        lowerBound = -maxPowerValue;
        this.motor = motor;
    }

    public PickerTest(String name, TatorMotor motor, double upperBound, double lowerBound, DutyCycleEncoder dutyCycleEncoder) {
        this(name, dutyCycleEncoder,motor);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.motor = motor;
    }

    public PickerTest(String name, TatorMotor motor, PowerDistribution pdp, int motorChannel, DutyCycleEncoder dutyCycleEncoder) {
        this(name, dutyCycleEncoder,motor);
        this.pdp = pdp;
        this.motorChannel = motorChannel;
        this.motor = motor;
    }

    public PickerTest(String name, DutyCycleEncoder encoder, TatorMotor motor){
        super(name);
        this.encoder = encoder;
        this.motor = motor;
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
        else if (button == XBOXController.Button.kX) {
            printTestInfo("The motor encoder: " + motor.getPosition() + " Abslute Encoder: " + encoder.getAbsolutePosition());
        }
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
    public void onButtonUp(XBOXController.Button button) {
        if (button == XBOXController.Button.kSTART) half = false;
    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }

}
