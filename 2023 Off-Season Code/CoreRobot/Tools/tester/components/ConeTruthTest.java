package common.Tools.tester.components;

import common.Tools.tester.ManualTest;
import common.Controllers.XBOXController;
import edu.wpi.first.wpilibj.AnalogInput;
import common.Util.DigitalSensor;

public class ConeTruthTest extends ManualTest{
    private AnalogInput rightSensor;
    private AnalogInput leftSensor;
    private DigitalSensor midSensor;
    private double sensorThreshold;

    public ConeTruthTest(String name, AnalogInput rightSensor, AnalogInput leftSensor, DigitalSensor midSensor, double sensorThreshHold) {
        super(name);
        this.rightSensor = rightSensor;
        this.leftSensor = leftSensor;
        this.midSensor = midSensor;
        this.sensorThreshold = sensorThreshHold;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to get the state  of the cone from the sensors");
    }

    private String getSensorState( )
    {
//        return (sensor.getVoltage() > sensorThreshold);
        if(rightSensor.getVoltage() < sensorThreshold && leftSensor.getVoltage() < sensorThreshold && midSensor.get()==true){
            return "The cone is head first";
        }else if(midSensor.get()==true && leftSensor.getVoltage() > sensorThreshold && rightSensor.getVoltage() > sensorThreshold){
            return "The cone is butt first";
        }else if(midSensor.get()==true && leftSensor.getVoltage() > sensorThreshold && rightSensor.getVoltage() < sensorThreshold){
            return "The cone is but first but needs to be moved";
        }else if(midSensor.get()==true && leftSensor.getVoltage() < sensorThreshold && rightSensor.getVoltage() > sensorThreshold){
            return "The cone is but first but needs to be moved";
        }else {
            return "There is no cone or the cone is too far away from the sensors.";
        }
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo("Sensors Cone state: " + getSensorState());

        }
    }
}
