package common.Tools.tester.components;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;
import com.ctre.phoenix6.hardware.TalonFX;

public class TalonFXPowerTest extends ManualTest {
    protected TalonFX motor;
    private double axisValue;
    private double upperBound = 1;
    private double lowerBound = -1;

    public TalonFXPowerTest(String name, TalonFX motor) {
        super(name);
        this.motor = motor;
    }

    public TalonFXPowerTest(String name, TalonFX motor, double upperBound, double lowerBound) {
        this(name, motor);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    @Override
    public void start() {
        printTestInstructions("Push joystick in direction to move (forward +, backward -)");
        printTestInstructions("Left bumper to print RPM");
        axisValue = 0;
    }

    private double getRawSpeed() {
        return axisValue;
    }

    private double getSpeed() {
        double speed = getRawSpeed();
        speed = (speed > upperBound ? upperBound : speed);
        speed = (speed < lowerBound ? lowerBound : speed);
        return speed;
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
        if (button == XBOXController.Button.kBUMPER_LEFT) {
            //logger.info("RPM is {}", motor.get());
            System.out.println("RPM is " + motor.getVelocity());
        } 
    }

//    @Override
//    public void onButtonUp(LogitechF310.Button button) {
//        if (button == LogitechF310.Button.START) half = false;
//    }

    @Override
    public void updateRightAxis(double value) {
        axisValue = value;
    }
}
