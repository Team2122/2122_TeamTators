package common.Tools.tester.components;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

import common.Util.TatorPigeon;

public class TatorPigeonTest extends ManualTest{
    private TatorPigeon tatorPigeon;

    public TatorPigeonTest(String name, TatorPigeon tatorPigeon) {
        super(name);
        this.tatorPigeon = tatorPigeon;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to display the current yaw, 'Y' for rotation");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kY) {
            printTestInfo(String.format("Yaw is: " + tatorPigeon.getYawContinuous()));
        }
        if (button == XBOXController.Button.kA) {
            printTestInfo(String.format("Rotation is: " + tatorPigeon.getRotation2d()));
        }
    }
}
