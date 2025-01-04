package frc.robot.subsystems.swerve;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.Volts;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.system.plant.LinearSystemId;
import edu.wpi.first.units.measure.LinearVelocity;
import edu.wpi.first.units.measure.Velocity;
import edu.wpi.first.wpilibj.simulation.DCMotorSim;
import frc.robot.subsystems.swerve.Module.SwerveModuleMotor;
import frc.robot.subsystems.swerve.SwerveConstants.SwerveModuleConstants;

public class ModuleIOSim implements ModuleIO {
    private DCMotorSim driveSim;
    private DCMotorSim steerSim;
    private PIDController steerController;
    private PIDController driveController;
    private SimpleMotorFeedforward driveFF;

    public ModuleIOSim() {
        driveSim = new DCMotorSim(
            LinearSystemId.createDCMotorSystem(
                DCMotor.getKrakenX60(1), 
                .025, // these numbers came from 6328's inertia values
                SwerveModuleConstants.kDriveGearing
            ),
            DCMotor.getKrakenX60(1),
            .001, .001
        );

        steerSim = new DCMotorSim(
            LinearSystemId.createDCMotorSystem(
                DCMotor.getKrakenX60(1), 
                .004, // these numbers came from 6328's inertia values
                SwerveModuleConstants.kSteerGearing
            ),
            DCMotor.getKrakenX60(1),
            .001, .001
        );

        steerController = new PIDController(4.5, 0, .05);
        steerController.enableContinuousInput(-Math.PI, Math.PI);

        driveController = new PIDController(0.259, 0, 0);
        driveFF = new SimpleMotorFeedforward(0, 1);
    }

    public void updateInputs(ModuleIOInputs inputs) {
        driveSim.update(.02);
        steerSim.update(.02);

        inputs.azimuthVelocityRPM = steerSim.getAngularVelocityRPM();
        inputs.azimuthPositionRotations = steerSim.getAngularPositionRotations();

        // special mod function that works with negatives
        // -0.5 because the range on the real cancoder is [-0.5, 0.5), not [0, 1.0)
        inputs.cancoderAbsolutePosition = (inputs.azimuthPositionRotations % 1 + 1) % 1 - 0.5;
        inputs.cancoderVelocity = inputs.azimuthVelocityRPM / 60;

        inputs.driveMotorTemp = 0;
        inputs.drivePositionRotations = driveSim.getAngularPositionRotations();
        inputs.driveVelocityRPS = driveSim.getAngularVelocityRPM() / 60;
        inputs.driveStatorCurrent = driveSim.getCurrentDrawAmps();
        inputs.driveSupplyCurrent = driveSim.getCurrentDrawAmps();
        inputs.driveTorqueCurrent = driveSim.getCurrentDrawAmps();
    }

    public void setSetpoint(SwerveModuleState state) {
        double driveSpeed = driveSim.getAngularVelocityRPM()/60;
        double setpointSpeed = Module.metersToRotations(state.speedMetersPerSecond);
        double driveFeedback = driveController.calculate(driveSpeed, setpointSpeed);
        LinearVelocity stateSpeed = MetersPerSecond.of(state.speedMetersPerSecond);
        driveSim.setInput(-(driveFeedback + driveFF.calculate(stateSpeed).in(Volts)));
        
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
