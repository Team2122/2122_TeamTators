package frc.robot.subsystems.chamberOfCorals;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;
import org.teamtators.util.QuickDebug;

public class ChamberOfCoralsIOSim implements ChamberOfCoralsIO {
    private FlywheelSim sim;

    public ChamberOfCoralsIOSim() {
        // actually a minion, but those aren't in DCMotor, so Neo550 it is
        sim =
                new FlywheelSim(
                        LinearSystemId.createFlywheelSystem(DCMotor.getNeo550(1), 0.001, 1),
                        DCMotor.getNeo550(1),
                        0);
    }

    public void updateInputs(ChamberOfCoralsIOInputs inputs) {
        inputs.breakbeamTriggered = QuickDebug.input("Chamber/Sensor", false);

        inputs.falconMotorConnected = true;
        inputs.minion1MotorConnected = true;
        inputs.minion2MotorConnected = true;
    }

    public void setVolts(double falconVolts, double minionVolts) {
        sim.setInput(falconVolts);
    }
}
