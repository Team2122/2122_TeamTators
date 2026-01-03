package frc.robot.subsystems.swerve;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;
import frc.robot.subsystems.swerve.SwerveConstants.SwerveModuleConstants;
import org.teamtators.util.TatorMath;

public class ModuleIOSim implements ModuleIO {
    private DCMotorSim driveSim;
    private DCMotorSim steerSim;
    private PIDController steerController;
    private PIDController driveController;
    private SimpleMotorFeedforward driveFF;

    public ModuleIOSim() {
        driveSim =
                new DCMotorSim(
                        LinearSystemId.createDCMotorSystem(
                                DCMotor.getKrakenX60(1),
                                .025, // these numbers came from 6328's inertia values
                                SwerveModuleConstants.kDriveGearing),
                        DCMotor.getKrakenX60(1),
                        .001,
                        .001);

        steerSim =
                new DCMotorSim(
                        LinearSystemId.createDCMotorSystem(
                                DCMotor.getKrakenX60(1),
                                .004, // these numbers came from 6328's inertia values
                                SwerveModuleConstants.kSteerGearing),
                        DCMotor.getKrakenX60(1),
                        .001,
                        .001);

        steerController = new PIDController(4.5, 0, .05);
        steerController.enableContinuousInput(-Math.PI, Math.PI);

        driveController = new PIDController(2.2, 0, 0);
        driveFF = new SimpleMotorFeedforward(0, 0.64);
    }

    public void updateInputs(ModuleIOInputs inputs) {
        driveSim.update(.02);
        steerSim.update(.02);

        inputs.azimuthVelocityRPM = steerSim.getAngularVelocityRPM();
        inputs.azimuthPositionRotations = steerSim.getAngularPositionRotations();

        inputs.cancoderAbsolutePosition = TatorMath.mod(inputs.azimuthPositionRotations, 1);
        inputs.cancoderVelocity = inputs.azimuthVelocityRPM / 60;

        inputs.driveMotorTemp = 0;
        inputs.drivePositionRotations = driveSim.getAngularPositionRotations();
        inputs.driveVelocityRPS = driveSim.getAngularVelocityRPM() / 60;
        inputs.driveStatorCurrent = driveSim.getCurrentDrawAmps();
        inputs.driveSupplyCurrent = driveSim.getCurrentDrawAmps();
        inputs.driveTorqueCurrent = driveSim.getCurrentDrawAmps();

        inputs.odometryTimestamps = new double[] {Timer.getTimestamp()};
        inputs.odometryAzimuthPositionsRotations = new double[] {inputs.azimuthPositionRotations};
        inputs.odometryDrivePositionsRotations = new double[] {inputs.drivePositionRotations};
    }

    public void setSetpoint(SwerveModuleState state) {
        double driveSpeed = driveSim.getAngularVelocityRPM() / 60;
        double setpointSpeed = Module.metersToRotations(state.speedMetersPerSecond);
        double driveFeedback = driveController.calculate(driveSpeed, setpointSpeed);
        double volts = MathUtil.clamp(driveFeedback + driveFF.calculate(setpointSpeed), -48, 48);
        driveSim.setInput(volts);

        steerController.setSetpoint(state.angle.getRadians());
        steerSim.setInput(steerController.calculate(steerSim.getAngularPositionRad()));
    }

    public void setPDSV(double P, double D, double S, double V, SwerveModuleMotor target) {
        if (target == SwerveModuleMotor.DRIVE) {
            driveController.setP(P);
            driveController.setD(D);
            driveFF = new SimpleMotorFeedforward(S, V);
        } else {
            steerController.setP(P);
            steerController.setD(D);
        }
    }
}
