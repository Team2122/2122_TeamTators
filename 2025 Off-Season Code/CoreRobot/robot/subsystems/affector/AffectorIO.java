package frc.robot.subsystems.affector;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface AffectorIO {
    @AutoLog
    public class AffectorIOInputs {
        public double motorSpeedRPS;
        public double supplyCurrent;
        public double statorCurrent;
        public double tempCelsius;
        public double appliedVolts;
        public boolean connected = true;
    }

    public default void updateInputs(AffectorIOInputs inputs) {}

    public default void setVoltage(double volts) {}

    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Affector");
    }
}
