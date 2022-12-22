package org.teamtators.Tools.tester.components;

import com.revrobotics.RelativeEncoder;

import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;

public class CANEncoderTest extends ManualTest {

    private RelativeEncoder encoder;

    public CANEncoderTest(String name, RelativeEncoder encoder) {
        super(name);
        this.encoder = encoder;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current values, 'B' to reset the encoder values");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kB) {
            encoder.setPosition(0);
            printTestInfo("Encoder reset");
        } else if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("ticks: %.3f, Rate: %.3f, Rotations: %.3f ",
                    encoder.getCountsPerRevolution() * encoder.getPosition(), encoder.getVelocity(), encoder.getPosition()));
        }
    }
}
