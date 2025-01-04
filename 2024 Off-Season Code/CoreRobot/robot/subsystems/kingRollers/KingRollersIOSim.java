package frc.robot.subsystems.kingRollers;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;

import org.teamtators.Util.QuickDebug;

public class KingRollersIOSim implements KingRollersIO {
    private FlywheelSim motorSim;

    private double volts;

    public KingRollersIOSim() {
        motorSim = new FlywheelSim(
            LinearSystemId.createFlywheelSystem(
                DCMotor.getFalcon500(1),
                .001,
                1
            ),
            DCMotor.getFalcon500(1),
            .0001
        );
    }

    @Override
    public void updateInputs(KingRollersIOInputs inputs) {
        motorSim.setInput(volts);
        motorSim.update(.02);

        inputs.appliedVolts = volts;
        inputs.controlMode = "VoltageOut";
        inputs.dutyCycle = 0;
        inputs.statorCurrent = motorSim.getCurrentDrawAmps();
        inputs.supplyCurrent = motorSim.getCurrentDrawAmps();
        inputs.torqueCurrent = motorSim.getCurrentDrawAmps();
        inputs.tempCelcius = -1;
        inputs.velocityRPS = motorSim.getAngularVelocityRPM() / 60;

        inputs.noteSensor = QuickDebug.input("KingRollers/Exit Sensor", false);
        inputs.safetySensor = QuickDebug.input("KingRollers/Safety Sensor", false);
    }

    @Override
    public void setSpeed(KingRollers.Speeds speed) {
        volts = speed.kVolts;
    }
}
