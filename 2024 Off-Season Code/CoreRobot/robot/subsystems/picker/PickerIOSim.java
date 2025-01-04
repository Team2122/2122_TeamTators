package frc.robot.subsystems.picker;

import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.simulation.FlywheelSim;

import org.teamtators.Util.QuickDebug;

public class PickerIOSim implements PickerIO {
    FlywheelSim motorSim;
    double volts = 0;

    public PickerIOSim() {
        motorSim = new FlywheelSim(
            LinearSystemId.createFlywheelSystem(
                DCMotor.getKrakenX60(1),
                .01,
                1
            ),
            DCMotor.getKrakenX60(1),
            .001
        );
    }

    @Override
    public void updateInputs(PickerIOInputs inputs) {
        motorSim.setInput(volts);
        motorSim.update(.02);

        inputs.appliedVolts = volts;
        /*inputs.chimneySensorActivated = false;
        inputs.farEntranceActivated = false;*/
        inputs.controlMode = "VoltageOut";
        inputs.dutyCycle = 0;
        inputs.statorCurrent = motorSim.getCurrentDrawAmps();
        inputs.supplyCurrent = motorSim.getCurrentDrawAmps();
        inputs.torqueCurrent = motorSim.getCurrentDrawAmps();
        inputs.tempCelcius = 0;
        inputs.velocityRPS = motorSim.getAngularVelocityRPM() / 60;

        inputs.farEntranceActivated = QuickDebug.input("Picker/Far Entrance", false);
        inputs.chimneySensorActivated = QuickDebug.input("Picker/Chimney", false);
    }

    @Override
    public void setSpeed(Picker.Speeds speed) {
        volts = speed.volts;
    }
}
