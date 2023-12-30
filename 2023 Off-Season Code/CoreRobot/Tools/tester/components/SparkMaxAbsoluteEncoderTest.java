package common.Tools.tester.components;

import com.revrobotics.SparkMaxAbsoluteEncoder;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

public class SparkMaxAbsoluteEncoderTest extends ManualTest{

    public SparkMaxAbsoluteEncoder encoder;
    public double relativeTo0 = 0;
    
    public SparkMaxAbsoluteEncoderTest(String name, SparkMaxAbsoluteEncoder absoluteEncoder) {
        super(name);
        encoder = absoluteEncoder;
    }

    public SparkMaxAbsoluteEncoderTest(String name, SparkMaxAbsoluteEncoder absoluteEncoder, double relativeTo0) {
        super(name);
        encoder = absoluteEncoder;
        this.relativeTo0 = relativeTo0;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current encoder distance");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("Distance is: " + (encoder.getPosition() - relativeTo0)));
        }
    }
}
