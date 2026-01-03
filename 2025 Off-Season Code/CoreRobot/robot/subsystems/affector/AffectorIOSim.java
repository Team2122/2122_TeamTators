package frc.robot.subsystems.affector;

import org.teamtators.util.QuickDebug;

public class AffectorIOSim implements AffectorIO {
    public void updateInputs(AffectorIOInputs inputs) {
        inputs.connected = true;
        inputs.statorCurrent = QuickDebug.input("Affector/StatorCurrent", 0.0);
    }
}
