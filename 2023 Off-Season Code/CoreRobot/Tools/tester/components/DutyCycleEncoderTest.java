package common.Tools.tester.components;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

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
        printTestInstructions("Press 'B' to reset the distance encoder");
        printTestInstructions("Press 'Y' to display the current absolute encoder value");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("Distance is: " + encoder.getAbsolutePosition()));
        }
        if (button == XBOXController.Button.kY) {
            printTestInfo(String.format("Absolute Encoder: " + encoder.getAbsolutePosition()));
        }
        if (button == XBOXController.Button.kB) {
            encoder.reset();
        }
    }
}
