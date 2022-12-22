package org.teamtators.Tools.tester.components;

import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.tester.ManualTest;
import org.teamtators.Util.CANCoderWrapper;

public class CANCoderTest extends ManualTest{

    private CANCoderWrapper canCoder;

    public CANCoderTest(String name, CANCoderWrapper canCoder) {
        super(name);
        this.canCoder = canCoder;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current values, 'B' to reset the encoder values");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kB) {
            canCoder.setPosition(0);
            printTestInfo("Encoder reset");
        } else if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("Absolute Position: %.3f",
                    canCoder.getConvertedAbsolute()));
        }
    }
    
}
