package frc.robot.subsystems.swerve;

import org.littletonrobotics.junction.AutoLog;
import org.teamtators.tester.ManualTest;

import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;

public interface ModuleIO {
    @AutoLog
    public class ModuleIOInputs {
        protected double drivePositionRotations;
        protected double driveVelocityRPS;
        protected double driveMotorTemp;
        protected double driveSupplyCurrent;
        protected double driveStatorCurrent;
        protected double driveTorqueCurrent;
        protected double driveAppliedVolts;
        protected boolean driveConnected = true;

        protected double azimuthPositionRotations;
        protected double azimuthVelocityRPM;
        protected double azimuthInputVoltage;
        protected double azimuthSupplyCurrent;
        protected double azimuthStatorCurrent;
        protected double azimuthTorqueCurrent;
        protected double azimuthMotorTemp;
        protected double azimuthAppliedVolts;
        protected boolean azimuthConnected = true;

        protected double cancoderAbsolutePosition;
        protected double cancoderVelocity;
        protected boolean cancoderConnected = true;

        protected double[] odometryTimestamps = new double[] {};
        protected double[] odometryAzimuthPositionsRotations = new double[] {};
        protected double[] odometryDrivePositionsRotations = new double[] {};
    }

    public default void updateInputs(ModuleIOInputs inputs) {}
    public default void setSetpoint(SwerveModuleState state) {}
    public default void zoom() {}
    public default void noZoom() {}

    /**
     * Configure a motor's closed and open loop parameters. Mainly just used for
     * tuning. See
     * <a href="https://github.com/Team2122/documentation/blob/main/PID%20Info.md">our docs</a>
     * for more info
     *
     * @param P The proportional gain for the feedback controller
     * @param D The derivative gain for the feedback controller
     * @param S The static feedforward gain
     * @param V The velocity feedforward gain
     * @param target The motor whose parameters should be configured
     */
    public default void setPDSV(double P, double D, double S, double V, SwerveModuleMotor target) {}
	public default ManualTest getManualTest() {
		return new ManualTest("Replay/Sim Module") {};
	}
}
