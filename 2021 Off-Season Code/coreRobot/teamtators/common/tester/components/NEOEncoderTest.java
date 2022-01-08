package org.teamtators.common.tester.components;

import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.NEOEncoder;
import org.teamtators.common.tester.ManualTest;

public class NEOEncoderTest extends ManualTest {

    private NEOEncoder encoder;

    public NEOEncoderTest(String name, NEOEncoder encoder) {
        super(name);
        this.encoder = encoder;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current values, 'B' to reset the encoder values");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.B) {
            encoder.reset();
            printTestInfo("Encoder reset");
        } else if (button == LogitechF310.Button.A) {
            printTestInfo(String.format("Distance: %.3f (ticks: %d), Rate: %.3f, Rotations: %.3f ",
                    encoder.getDistance(), encoder.getCounts(), encoder.getRate(), encoder.getRotations()));
        }
    }
}
