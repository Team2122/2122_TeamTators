package org.teamtators.common.tester.components;

import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.PressureSensor;
import org.teamtators.common.tester.ManualTest;

public class PressureSensorTest extends ManualTest {
    private PressureSensor sensor;

    public PressureSensorTest(String name, PressureSensor sensor) {
        super(name);
        this.sensor = sensor;
    }

    @Override
    public void start() {
        printTestInstructions("Press A to log pressure.");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.A) {
            printTestInfo("{} ({} V)", sensor.getPressure(), sensor.getVoltage());
        }
    }
}
