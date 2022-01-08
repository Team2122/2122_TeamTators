package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.BuiltInAccelerometer;
import edu.wpi.first.wpilibj.interfaces.Accelerometer;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

public class BuiltInAccelerometerTest extends ManualTest {
    private final BuiltInAccelerometer accelerometer;

    public BuiltInAccelerometerTest(String name, BuiltInAccelerometer accelerometer) {
        super(name);
        this.accelerometer = accelerometer;
    }

    @Override
    public void start() {
        super.start();
        printTestInstructions("Press A to print info. Press X to set range to 2G. Press Y to set range to 4G. Press B to set range to 8G.");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        switch (button) {
            case A:
                double x = accelerometer.getX();
                double y = accelerometer.getY();
                double z = accelerometer.getZ();
                printTestInfo("X {}, Y {}, Z {}", x, y, z);
                printTestInfo("{}", 180 * (Math.acos(z))/ 3.1415926);
                printTestInfo("Tilted towards the {}", x < 0 ? "front" : "back");
                break;
            case X:
                accelerometer.setRange(Accelerometer.Range.k2G);
                printTestInfo("Set range to 2G");
                break;
            case Y:
                accelerometer.setRange(Accelerometer.Range.k4G);
                printTestInfo("Set range to 4G");
                break;
            case B:
                accelerometer.setRange(Accelerometer.Range.k8G);
                printTestInfo("Set range to 8G");
               break;
        }
    }
}
