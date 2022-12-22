package org.teamtators.Tools.tester.components;

import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;

import edu.wpi.first.wpilibj.DutyCycleEncoder;

public class DutyCycleEncoderTest extends ManualTest{
    private DutyCycleEncoder encoder;

    public DutyCycleEncoderTest(String name, DutyCycleEncoder encoder) {
        super(name);
        this.encoder = encoder;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current encoder distance");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("Distance is: " + encoder.getDistance()));
        }
    }
}
