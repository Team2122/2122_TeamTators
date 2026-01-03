package frc.robot.subsystems.climber;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface ClimberIO {
    @AutoLog
    public class ClimberIOInputs {

        public double currentPosition;
        public double supplyCurrent;
        public double statorCurrent;
        public double tempCelsius;
        public double appliedVolts;

        public boolean motorConnected = true;
    }

    public default void setVolts(double volts) {}

    public default void calibrateEncoder() {}

    public default void updateInputs(ClimberIOInputs inputs) {}

    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Climber") {};
    }
}
