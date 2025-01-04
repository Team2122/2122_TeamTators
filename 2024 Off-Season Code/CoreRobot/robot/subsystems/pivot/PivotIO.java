package frc.robot.subsystems.pivot;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface PivotIO {
    @AutoLog
    public class PivotIOInputs {
        protected double supplyCurrent;
        protected double statorCurrent;
        protected double torqueCurrent;
        protected double dutyCycle;
        protected String controlMode;
        protected double positionRotations;
        protected double velocityRPS;
        protected double tempCelcius;
        protected double appliedVolts;
        protected boolean connected = true;

        protected double absoluteEncoderPosition;
    }

    public default void updateInputs(PivotIOInputs inputs) {}

    public default void setEncoderPosition(double value) {}

    public default void setSetpoint(double theta) {}

    public default void setVolts(double volts) {}

    public default void updateControls() {}
    public default ManualTestGroup getManualTest() {
        return new ManualTestGroup("Pivot") {};
    }
}