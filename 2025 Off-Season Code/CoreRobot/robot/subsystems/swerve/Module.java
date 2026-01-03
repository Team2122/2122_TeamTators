package frc.robot.subsystems.swerve;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.constants.Constants;
import frc.robot.subsystems.swerve.SwerveConstants.SwerveModuleConstants;
import frc.robot.subsystems.swerve.SwerveDrive.ModuleFramePositions;
import org.littletonrobotics.junction.Logger;
import org.teamtators.tester.ManualTest;

public class Module {
    public enum SwerveModuleMotor {
        DRIVE,
        AZIMUTH
    }

    private ModuleIO io;
    private ModuleIOInputsAutoLogged inputs = new ModuleIOInputsAutoLogged();

    private SwerveModuleState state;
    private SwerveModulePosition[] odometryPositions;

    private ModuleFramePositions framePosition;

    public Module(ModuleFramePositions modulePosition, int driveId, int azimuthId, int cancoderId) {
        this.framePosition = modulePosition;

        switch (Constants.kRobotMedium) {
            case REAL:
                io = new ModuleIOReal(modulePosition, driveId, azimuthId, cancoderId);
                // io = new ModuleIO() {};
                // ^-- useful for disabling modules, e.g. when running on a table
                break;
            case SIM:
                io = new ModuleIOSim();
                break;
            case REPLAY:
                io = new ModuleIO() {};
                break;
        }

        state = new SwerveModuleState();
        odometryPositions = new SwerveModulePosition[] {};
    }

    public void setMotion(SwerveModuleState state) {
        // This is causing issues with positive drive power not rotating the
        // same speed as negative power, but also causes rotation algorithms,
        // such as aiming at a target or our algorithm to keep the bot's
        // rotation stable while driving, to spin the wheels rapidly, making the
        // robot jitter and making the rotation unstable as well.  In the end,
        // this optimization is needed, as we can correct for the inconsistent
        // drive speeds but we need rotation to work without jittering.
        Rotation2d azimuthPosition = Rotation2d.fromRotations(inputs.azimuthPositionRotations);
        state.optimize(azimuthPosition);
        io.setSetpoint(state);
    }

    public void updateLogs() {
        io.updateInputs(inputs);

        Logger.processInputs("Swerve/" + framePosition, inputs);

        // Calculate positions for odometry
        int sampleCount = inputs.odometryTimestamps.length; // All signals are sampled together
        odometryPositions = new SwerveModulePosition[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            double positionMeters = rotationsToMeters(inputs.odometryDrivePositionsRotations[i]);
            double angleRotations = inputs.odometryAzimuthPositionsRotations[i];
            odometryPositions[i] =
                    new SwerveModulePosition(positionMeters, Rotation2d.fromRotations(angleRotations));
        }
    }

    public double[] getOdometryTimestamps() {
        return inputs.odometryTimestamps;
    }

    public SwerveModulePosition[] getOdometryPositions() {
        return odometryPositions;
    }

    public Rotation2d getAngle() {
        return Rotation2d.fromRotations(inputs.cancoderAbsolutePosition);
    }

    public SwerveModuleState getCurrentState() {
        state.angle = Rotation2d.fromRotations(inputs.cancoderAbsolutePosition);
        state.speedMetersPerSecond = rotationsToMeters(inputs.driveVelocityRPS);
        return state;
    }

    public void testing_setPDSV(double P, double D, double S, double V, SwerveModuleMotor target) {
        io.setPDSV(P, D, S, V, target);
    }

    public static double metersToRotations(double meters) {
        double conversionFactor =
                SwerveModuleConstants.kDriveGearing / SwerveModuleConstants.kCircumference;
        return meters * conversionFactor;
    }

    public static double rotationsToMeters(double rotations) {
        double conversionFactor =
                SwerveModuleConstants.kCircumference / SwerveModuleConstants.kDriveGearing;
        return rotations * conversionFactor;
    }

    public ManualTest getManualTest() {
        return io.getManualTest();
    }

    public boolean getHealth() {
        return inputs.azimuthConnected && inputs.cancoderConnected && inputs.driveConnected;
    }
}
