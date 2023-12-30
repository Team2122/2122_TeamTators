package common.Tools.tester.components;

import common.Tools.tester.ManualTest;
import common.Controllers.XBOXController;
import edu.wpi.first.wpilibj.AnalogInput;

public class AnalogInputSensorTest extends ManualTest{
    private AnalogInput sensor;
    private double sensorThreshold;

    public AnalogInputSensorTest(String name, AnalogInput sensor, double sensorThreshhold) {
        super(name);
        this.sensor = sensor;
        this.sensorThreshold = sensorThreshhold;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to get the value and type from the sensor");
    }

    private boolean getSensorState( )
    {
        return (sensor.getVoltage() > sensorThreshold);
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo("Sensor voltage: " + sensor.getVoltage() + "Sensor State: " + getSensorState());

        }
    }
}
