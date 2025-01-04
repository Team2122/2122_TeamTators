package frc.robot.subsystems.climber;

import org.teamtators.tester.ManualTestGroup;

import org.littletonrobotics.junction.AutoLog;

public interface ClimberIO {
    @AutoLog
    public class ClimberIOInputs {
        protected double supplyCurrent;
        protected double statorCurrent;
        protected double torqueCurrent;
        protected double dutyCycle;
        protected String controlMode;
        protected double motorVelocityRPS;
        protected double motorPositionRotations;
        protected double tempCelcius;
        protected double appliedVolts;
        protected boolean connected = true;

        protected boolean downSensorHit;
        protected boolean upSensorHit;

        protected double cancoderPosition;
        protected double cancoderVelocity;
    }

    public default void updateInputs(ClimberIOInputs inputs) {}
    public default void setSetpoint(Climber.Position position) {};
    public default void setVoltage(double volts) {};
    public default void setEncoderPosition(double motorRotations) {};
    public default ManualTestGroup getManualTest() {
        return new ManualTestGroup("Climber") {};
    }
}
