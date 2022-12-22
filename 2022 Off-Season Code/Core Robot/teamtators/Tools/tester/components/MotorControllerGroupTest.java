package org.teamtators.Tools.tester.components;

import edu.wpi.first.wpilibj.motorcontrol.MotorController;

import org.teamtators.Controllers.XBOXController;
import org.teamtators.Tools.datalogging.DataCollector;
import org.teamtators.Tools.datalogging.DataLoggable;
import org.teamtators.Tools.datalogging.LogDataProvider;
import org.teamtators.Tools.tester.ManualTest;
import org.teamtators.Util.MotorControllerGroup;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public class MotorControllerGroupTest extends ManualTest implements DataLoggable {
    private final MotorControllerGroup group;
    private MotorController controller;
    private int selected = 0;
    private double axisValue;
    private final DataCollector dataCollector = DataCollector.getDataCollector();
    private LogDataProvider logDataProvider = new VelocityPowerLogProvider();
    private boolean collecting = false;
    private boolean half = false;

    private BooleanSupplier fwdLimit;
    private BooleanSupplier revLimit;

    public MotorControllerGroupTest(String name, MotorControllerGroup group) {
        super(name);
        this.group = group;
    }

    public MotorControllerGroupTest(String name, MotorControllerGroup group, BooleanSupplier fwdLimit, BooleanSupplier revLimit) {
        this(name, group);
        this.fwdLimit = fwdLimit;
        this.revLimit = revLimit;
    }

    @Override
    public void start() {
        String testInstructions;
        testInstructions = "Press 'A' to cycle motor. Press 'B' to select all motors. Push joystick in direction to move (forward +, backward -). Press start to run at 50%";
        printTestInstructions(testInstructions);
        selected = 0;
        axisValue = 0;
        group.enableFollowerMode();
        controller = group;
        half = false;
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        switch (button) {
            case kA:
                int max = group.getSpeedControllers().length - 1;
                if (selected + 1 > max) {
                    selected = 0;
                } else {
                    selected++;
                }
                printTestInfo("Selected motor {}", selected);
                group.disableFollowerMode(); //to reset all of them
                controller = group.getSpeedControllers()[selected];
                controller.set(0);
                break;
            case kB:
                group.enableFollowerMode();
                controller = group;
                printTestInfo("Selected master");
                break;
            case kSTART:
                half = true;
        }
    }

    @Override
    public void onButtonUp(XBOXController.Button button) {
        if (button == XBOXController.Button.kSTART) {
            half = false;
        }
    }

    @Override
    public void stop() {
        group.set(0);
        group.enableFollowerMode();
    }

    private boolean loggedRecently = false;

    @Override
    public void update(double delta) {
        double pow = getPower();
        if (fwdLimit != null && fwdLimit.getAsBoolean()) {
            if (pow > 0) {
                pow = 0;
                if (!loggedRecently) {
                    printTestInfo("DANGER! FWD LIMIT TRIPPED!");
                    loggedRecently  = true;
                }
            }
        } else if (revLimit != null && revLimit.getAsBoolean()) {
            if (pow < 0) {
                pow = 0;
                if(!loggedRecently) {
                    printTestInfo("DANGER! REV LIMIT TRIPPED!");
                    loggedRecently = true;
                }
            }
        } else {
            loggedRecently = false;
        }
        controller.set(pow);

    }

    @Override
    public void updateAxis(double value) {
        axisValue = value;
    }

    private double getPower() {
        return half ? 0.5 : axisValue;
    }

    @Override
    public LogDataProvider getLogDataProvider() {
        return logDataProvider;
    }

    private class VelocityPowerLogProvider implements LogDataProvider {

        @Override
        public String getName() {
            return MotorControllerGroupTest.this.getName();
        }

        @Override
        public List<Object> getKeys() {
            return Arrays.asList("output", "velocity");
        }

        @Override
        public List<Object> getValues() {
            return Arrays.asList(axisValue, group.getVelocity());
        }
    }
}
