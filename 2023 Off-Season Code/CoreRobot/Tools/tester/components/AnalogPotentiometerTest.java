package common.Tools.tester.components;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

import common.Util.AnalogPotentiometer;

public class AnalogPotentiometerTest extends ManualTest{
    private AnalogPotentiometer analogPotentiometer;

    public AnalogPotentiometerTest(String name, AnalogPotentiometer analogPotentiometer) {
        super(name);
        this.analogPotentiometer = analogPotentiometer;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current encoder distance");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("Distance is: " + analogPotentiometer.get() + " voltage is " + analogPotentiometer.getRawVoltage()));
        }
    }
}
