package frc.robot.subsystems.overwatch.pivot;

import static edu.wpi.first.units.Units.RadiansPerSecond;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.units.measure.AngularVelocity;
import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTestGroup;

public interface PivotIO {
    @AutoLog
    public class PivotIOInputs {
        public boolean motorConnected = true;
        public Rotation2d position = Rotation2d.kCW_90deg;
        public AngularVelocity velocity = RadiansPerSecond.of(0);
        public double supplyCurrent;
        public double statorCurrent;
        public double tempCelsius;
        public double appliedVolts;

        public boolean canCoderConnected = true;
        public double cancoderPositionRotations;

        public PivotControlMode controlMode = PivotControlMode.NORMAL_OPERATION;
    }

    public enum PivotControlMode {
        NORMAL_OPERATION,
        HOLDING_ALGAE,
        NO_VELOCITY_FEEDFORWARD,
    }

    public default void updateInputs(PivotIOInputs inputs) {}

    public default void setSetpoint(Rotation2d angle, double veloityRPS) {}

    public default void setControlMode(PivotControlMode controlMode) {}

    public default ManualTestGroup getManualTests() {
        return new ManualTestGroup("Pivot") {};
    }
}
