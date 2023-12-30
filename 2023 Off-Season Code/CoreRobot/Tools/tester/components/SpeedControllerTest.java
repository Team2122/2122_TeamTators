package common.Tools.tester.components;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;

public class SpeedControllerTest extends ManualTest {

    protected MotorController motor;
    protected MotorController lMotor;
    protected MotorController rMotor;
    
    private boolean half;
    private double axisValue;
    private PowerDistribution pdp;
    private int motorChannel = -1;
    private double upperBound = 1;
    private double lowerBound = -1;

    public SpeedControllerTest(String name, MotorController motor) {
        super(name);
        this.motor = motor;
        
    }

    public SpeedControllerTest(String name, MotorController motor, double maxPowerValue) {
        super(name);
        this.motor = motor;
        upperBound = maxPowerValue;
        lowerBound = -maxPowerValue;
    }

    public SpeedControllerTest(String name, MotorController motor, double upperBound, double lowerBound) {
        this(name, motor);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    public SpeedControllerTest(String name, MotorController motor, PowerDistribution pdp, int motorChannel) {
        this(name, motor);
        this.pdp = pdp;
        this.motorChannel = motorChannel;
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
        if (motorChannel == -1) {
            return 0;
        }
        else {
            return pdp.getCurrent(motorChannel); // check if this is right
        }
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
    public void onButtonDown(XBOXController.Button button) {
        if(button == XBOXController.Button.kSTART) half = true;
        else if (button == XBOXController.Button.kA && pdp != null) {
            System.out.println("Total current usage: " + this.getCurrent());
            //logger.info("Total current usage: " + this.getCurrent());
        }
    }

    @Override
    public void onButtonUp(XBOXController.Button button) {
        if (button == XBOXController.Button.kSTART) half = false;
    }

    @Override
    public void updateRightAxis(double value) {
        axisValue = value;
    }
}
