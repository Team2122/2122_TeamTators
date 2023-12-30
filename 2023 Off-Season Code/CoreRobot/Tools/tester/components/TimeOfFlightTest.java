package common.Tools.tester.components;

import com.playingwithfusion.TimeOfFlight;

import com.playingwithfusion.jni.TimeOfFlightJNI;
import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

public class TimeOfFlightTest extends ManualTest {
    private final TimeOfFlight timeOfFlight;

    public TimeOfFlightTest(String name, TimeOfFlight timeOfFlight) {
        super(name);
        this.timeOfFlight = timeOfFlight;
    }

    @Override
    public void start() {
        printTestInstructions("Press 'A' to get the distance from the target");
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        if (button == XBOXController.Button.kA) {
            printTestInfo("Range to target: " + timeOfFlight.getRange());
        }
    }
}
