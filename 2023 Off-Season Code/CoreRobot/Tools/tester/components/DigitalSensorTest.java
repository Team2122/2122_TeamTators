package common.Tools.tester.components;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;
import common.Util.DigitalSensor;

public class DigitalSensorTest extends ManualTest {

    private DigitalSensor digitalSensor;

    public DigitalSensorTest(String name, DigitalSensor digitalSensor) {
        super(name);
        this.digitalSensor = digitalSensor;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to get the value and type from the sensor");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo("Digital sensor value: " + digitalSensor.get() + "    Type: " + digitalSensor.getType());

        }
    }
}
