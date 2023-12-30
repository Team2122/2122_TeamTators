package common.Tools.tester.components;


import edu.wpi.first.wpilibj.motorcontrol.PWMVictorSPX;
import common.Tools.tester.ManualTest;

public class PWMVictorSPXManualTest extends ManualTest{

    private PWMVictorSPX motor;
    private double axis;

    public PWMVictorSPXManualTest(String name, PWMVictorSPX motor){
        super(name);
        this.motor = motor;
    }

    @Override
    public void start() {
        printTestInfo("Testing a PWMVictorSPX");
        printTestInstructions("Use RIGHT joystick to test the power of the PWMVictorSPX");
    }

    @Override
    public void update(double delta) {
        motor.setVoltage(axis * 5);
    }

    @Override
    public void stop() {
        printTestInfo("Exiting PWMVictorSPX test");
    }

    @Override
    public void updateRightAxis(double value) {
        axis = value;
    }

}
