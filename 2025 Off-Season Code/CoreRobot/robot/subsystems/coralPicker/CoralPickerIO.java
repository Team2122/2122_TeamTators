package frc.robot.subsystems.coralPicker;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface CoralPickerIO {
    @AutoLog
    public class CoralPickerIOInputs {
        public boolean inductanceSensor;

        public double currentPosition;
        public double supplyCurrent;
        public double statorCurrent;
        public double tempCelsius;
        public double appliedVolts;

        public boolean motorConnected = true;
    }

    public default void setVolts(double volts) {}

    public default void setSetpoint(double rotations) {}

    public default void updateInputs(CoralPickerIOInputs inputs) {}

    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Coral Picker") {};
    }
}
