package frc.robot.subsystems.upperNotePath;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

import frc.robot.subsystems.upperNotePath.UpperNotePath.ShooterSpeeds;

public interface UpperNotePathIO {
    @AutoLog
    public class UpperNotePathIOInputs {
        protected double leftShooterSupplyCurrent;
        protected double leftShooterStatorCurrent;
        protected double leftShooterTorqueCurrent;
        protected double leftShooterDutyCycle;
        protected String leftShooterControlMode;
        protected double leftShooterVelocityRPS;
        protected double leftShooterTempCelcius;
        protected double leftShooterAppliedVolts;
        protected boolean leftShooterConnected = true;

        protected double rightShooterSupplyCurrent;
        protected double rightShooterStatorCurrent;
        protected double rightShooterTorqueCurrent;
        protected double rightShooterDutyCycle;
        protected String rightShooterControlMode;
        protected double rightShooterVelocityRPS;
        protected double rightShooterTempCelcius;
        protected double rightShooterAppliedVolts;
        protected double rightShooterPositionRotations;
        protected double rightShooterPositionDegrees;
        protected boolean rightShooterConnected = true;

        protected boolean diverterSensor;

        protected double dunkerDutyCycle;
        protected boolean dunkerMotorInverted;
        protected boolean dunkerSensor;
    }

    public default void updateInputs(UpperNotePathIOInputs inputs) {}
    public default void setShooterSpeeds(ShooterSpeeds speeds) {}
    public default void setShooterSpeeds(double left, double right) {}
    public default void updateControls() {}
    public default void flipDiverterUp() {}
    public default void holdDiverter() {}
    public default void setDunkerVoltage(double volts) {}
    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Upper Note Path") {};
    }
}
