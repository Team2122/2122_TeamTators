package org.teamtators.common.tester.components;

import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import org.teamtators.common.tester.ManualTest;

public class VictorSPXManualTest extends ManualTest {

    /**
     * @param name Name for the ManualTest
     */
    private VictorSPX motor;
    private double axis;

    public VictorSPXManualTest(String name, VictorSPX motor) {
         super(name);
        this.motor = motor;
    }

    @Override
    public void start() {
        printTestInfo("Testing a VictorSPX");
        printTestInstructions("Use RIGHT joystick to test the power of the VictorSPX");
    }

    @Override
    public void update(double delta) {
        motor.set(VictorSPXControlMode.PercentOutput, axis);
    }

    @Override
    public void stop() {
        printTestInfo("Exiting VictorSPX test");
    }

    @Override
    public void updateAxis(double value) {
        axis = value;
    }

}
