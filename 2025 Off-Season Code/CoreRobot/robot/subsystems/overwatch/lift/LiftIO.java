package frc.robot.subsystems.overwatch.lift;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface LiftIO {
    @AutoLog
    public class LiftIOInputs {
        protected double motorPositionRotations;
        protected double motorVelocityRPS;
        protected double motorMotorTemp;
        protected double motorSupplyCurrent;
        protected double motorStatorCurrent;
        protected double motorAppliedVolts;
        protected boolean motorConnected = true;

        protected double canrangeDistance;
        protected boolean canrangeConnected = true;
    }

    public default void updateInputs(LiftIOInputs inputs) {}

    public default void setSetpoint(double position, double motorRotationsPerSecond) {}

    public default void initEncoder(double position) {}

    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Lift") {};
    }
}
