package common.Tools.tester.components;

import edu.wpi.first.wpilibj.Solenoid;

import java.util.function.BooleanSupplier;

import common.Controllers.XBOXController;
import common.Tools.tester.ManualTest;

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
    public void onButtonDown(XBOXController.Button button) {
        if (mayShift == null || mayShift.getAsBoolean()) {
            if (button == XBOXController.Button.kA) {
                solenoid.set(true);
                printTestInfo("Solenoid activated");
            } else if (button == XBOXController.Button.kB) {
                solenoid.set(false);
                printTestInfo("Solenoid deactivated");
            }
        } else if (mayShift != null) {
            printTestInfo("CANNOT SHIFT!!!!");
        }
    }
}
