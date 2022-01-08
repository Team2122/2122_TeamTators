package org.teamtators.common.tester.components;

import edu.wpi.first.wpilibj.Solenoid;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.ManualTest;

import java.util.function.BooleanSupplier;

public class SolenoidTest extends ManualTest {
    private Solenoid solenoid;
    private BooleanSupplier mayShift;

    public SolenoidTest(String name, Solenoid solenoid) {
        super(name);
        this.solenoid = solenoid;
    }

    public SolenoidTest(String name, Solenoid solenoid, BooleanSupplier mayShift) {
        this(name, solenoid);
        this.mayShift = mayShift;
    }

    @Override
    public void start() {
        printTestInstructions("Press A to activate solenoid, B to deactivate");
    }

    @Override
    public void onButtonDown(LogitechF310.Button button) {
        if (mayShift == null || mayShift.getAsBoolean()) {
            if (button == LogitechF310.Button.A) {
                solenoid.set(true);
                printTestInfo("Solenoid activated");
            } else if (button == LogitechF310.Button.B) {
                solenoid.set(false);
                printTestInfo("Solenoid deactivated");
            }
        } else if (mayShift != null) {
            printTestInfo("CANNOT SHIFT!!!!");
        }
    }
}
