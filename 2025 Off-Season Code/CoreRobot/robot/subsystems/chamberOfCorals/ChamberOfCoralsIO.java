package frc.robot.subsystems.chamberOfCorals;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface ChamberOfCoralsIO {
    @AutoLog
    public class ChamberOfCoralsIOInputs {
        public boolean breakbeamTriggered;

        public double falconSupplyCurrent;
        public double falconStatorCurrent;
        public double falconTempCelsius;
        public double falconAppliedVolts;
        public double falconVelocity;

        public double minion1SupplyCurrent;
        public double minion1StatorCurrent;
        public double minion1TempCelsius;
        public double minion1AppliedVolts;
        public double minion1Velocity;

        public double minion2SupplyCurrent;
        public double minion2StatorCurrent;
        public double minion2TempCelsius;
        public double minion2AppliedVolts;
        public double minion2Velocity;

        public boolean falconMotorConnected = true;
        public boolean minion1MotorConnected = true;
        public boolean minion2MotorConnected = true;
    }

    public default void setVolts(double falconVolts, double minionVolts) {}

    public default void updateInputs(ChamberOfCoralsIOInputs inputs) {}

    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Chamber of Coral") {};
    }
}
