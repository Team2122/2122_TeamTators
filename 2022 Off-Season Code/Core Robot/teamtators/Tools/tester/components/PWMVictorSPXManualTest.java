package org.teamtators.Tools.tester.components;


import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import edu.wpi.first.wpilibj.motorcontrol.PWMVictorSPX;
import org.teamtators.Tools.tester.ManualTest;

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
    public void updateAxis(double value) {
        axis = value;
    }

}
