package org.teamtators.Tools.tester.components;

import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;

import edu.wpi.first.wpilibj.DutyCycleEncoder;
import org.teamtators.Util.AnalogPotentiometer;

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
