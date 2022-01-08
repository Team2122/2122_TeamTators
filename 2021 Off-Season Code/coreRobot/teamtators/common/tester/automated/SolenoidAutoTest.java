package org.teamtators.common.tester.automated;

import edu.wpi.first.wpilibj.Solenoid;
import org.teamtators.common.controllers.LogitechF310;
import org.teamtators.common.tester.AutomatedTest;
import org.teamtators.common.tester.AutomatedTestMessage;

public class SolenoidAutoTest extends AutomatedTest {
    private Solenoid solenoid;

    public SolenoidAutoTest(String name, Solenoid solenoid) {
        super(name, true);
        this.solenoid = solenoid;
    }

    @Override
    protected void initialize() {
        sendMessage("Press Right Trigger to fire solenoid.", AutomatedTestMessage.Level.INFO);
        sendMessage("Press Y if the solenoid is triggered successfully, skip test otherwise.", AutomatedTestMessage.Level.INFO);
    }

    @Override
    public boolean step() {
        if (lastButton == LogitechF310.Button.TRIGGER_RIGHT)
            solenoid.set(true);
        if (lastButton == LogitechF310.Button.Y) {
            sendMessage("Finished successfully", AutomatedTestMessage.Level.INFO);
        } else if (skipped()) {
            sendMessage("Solenoid failed, skipping test", AutomatedTestMessage.Level.ERROR);
        } else {
            return false;
        }
        return true;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        solenoid.set(false);
    }
}
