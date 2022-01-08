package org.teamtators.common.tester.components;

import com.ctre.phoenix.motorcontrol.SensorCollection;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.hw.SRXEncoder;
import org.teamtators.common.tester.ManualTest;

public class SRXEncoderTest extends ManualTest {
    private SRXEncoder encoder;

    public SRXEncoderTest(String name, SRXEncoder encoder) {
        super(name);
        this.encoder = encoder;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current values, 'B' to reset the encoder values. Press 'X' to display extended information.");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (button == LogitechF310.Button.B) {
            encoder.reset();
            printTestInfo("Encoder reset");
        } else if (button == LogitechF310.Button.A) {
            printTestInfo(String.format("Distance: %.3f (ticks: %d), Rate: %.3f",
                    encoder.getDistance(), encoder.get(), encoder.getVelocity()));
        } else if (button == LogitechF310.Button.X) {
            SensorCollection collection = encoder.getMotor().getSensorCollection();
            double pwidthpos = collection.getPulseWidthPosition();
            double pwidthvel = collection.getPulseWidthVelocity();
            double prisetorise = collection.getPulseWidthRiseToRiseUs();
            double prisetofall = collection.getPulseWidthRiseToFallUs();

            printTestInfo("Pulse Width Position: {}, Pulse Width Velocity: {}, Pulse Rise to Rise: {}, Pulse Rise to Fall: {}", pwidthpos, pwidthvel, prisetorise, prisetofall);

        }
    }
}
