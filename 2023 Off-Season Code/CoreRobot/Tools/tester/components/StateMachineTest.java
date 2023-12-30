package common.Tools.tester.components;

import java.util.List;
import java.util.function.Consumer;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;
import common.teamtators.Subsystem;
import common.Util.EnumUtils;

public class StateMachineTest<T extends Enum<T>> extends ManualTest {
    private int aryIndex = 0;
    private List<String> enums;
    private Class<T> enumType;
    private Subsystem subsystem;
    private Consumer<T> setStateCallback;

    public StateMachineTest(
            String name, Class<T> enumType,
            Subsystem subsystem,
            Consumer<T> setStateCallback) {
        super(name);
        this.enumType = enumType;
        this.enums = EnumUtils.enum2nameslist(enumType);
        this.subsystem = subsystem;
        this.setStateCallback = setStateCallback;
    }

    @Override
    public void start() {
        printTestInstructions(
                "Press A to activate subsystem doPeriodic, B to deactivate, Left and right bumpers to switch states");
        aryIndex = 0;
    }

    @Override
    public void onButtonDown(XBOXController.Button button) {
        switch (button) {
            case kA:
                printTestInfo(enums.get(aryIndex) + " activated");
                setStateCallback.accept(EnumUtils.name2const(enumType, enums.get(aryIndex)));
                subsystem.setTestSubsystem(true);
                break;
            case kB:
                printTestInfo(enums.get(aryIndex) + " deactivated");
                subsystem.setTestSubsystem(false);
                break;
            case kBUMPER_LEFT:
                if (aryIndex == 0) {
                    aryIndex = enums.size() - 1;
                } else {
                    aryIndex -= 1;
                }
                subsystem.setTestSubsystem(false);
                printTestInfo(enums.get(aryIndex));
                break;
            case kBUMPER_RIGHT:
                if (aryIndex == enums.size() - 1) {
                    aryIndex = 0;
                } else {
                    aryIndex += 1;
                }
                subsystem.setTestSubsystem(false);
                printTestInfo(enums.get(aryIndex));
                break;
            default:
                printTestInfo("button without a binding pressed");
                break;

        }
    }

    // example of setState method in a subsystem:
    /**
     * public void setState(String str)
     * if (str == "state1") {
     * setNewState(subsystemState.STATE1);
     * }
     * ...
     */

    @Override
    public void stop() {
        subsystem.setTestSubsystem(false);
        subsystem.reset();
    }

    private enum StateEnum {
        asfasf,
        sdagsdg;
    };

    public static void testyTestWheeeee() {
        new StateMachineTest<>(
                "null", StateEnum.class, null,
                StateMachineTest::testyTestWheeeeeHelper);
    }

    public static void testyTestWheeeeeHelper(StateEnum a) {

    }
}
