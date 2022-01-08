package org.teamtators.common.tester.components;

import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.RevColorSensor;
import org.teamtators.common.tester.ManualTest;

public class RevColorSensorTest extends ManualTest {
    private RevColorSensor sensor;

    public RevColorSensorTest(String name, RevColorSensor sensor) {
        super(name);
        this.sensor = sensor;
    }

    @Override
    public void start() {
        printTestInstructions("Press A to read a matched color. Press B to read a raw color. Press X to read proximity and IR.");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                var res = sensor.getMatchedColor();
                var name = sensor.getMatchedColorName();
                logger.info(String.format("Color: %s @ %.4f confidence", name, res.confidence));
                break;
            case B:
                var color = sensor.getColor();
                logger.info("R: {} G: {} B: {}", color.red, color.green, color.blue);
                break;
            case X:
                var prox = sensor.getProximity();
                var ir = sensor.getRawIR();
                logger.info(String.format("Proximity: %d, IR: %d", prox, ir));
                break;
        }
    }
}
